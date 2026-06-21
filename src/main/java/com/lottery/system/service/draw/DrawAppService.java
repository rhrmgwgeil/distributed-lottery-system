package com.lottery.system.service.draw;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.system.entity.Activity;
import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import com.lottery.system.entity.User;
import com.lottery.system.mq.DrawMessageProducer;
import com.lottery.system.repository.ActivityRepository;
import com.lottery.system.repository.DrawTicketRepository;
import com.lottery.system.repository.PrizeRepository;
import com.lottery.system.repository.UserRepository;
import com.lottery.system.enums.TicketStatus;
import com.lottery.system.service.draw.strategy.PrizeProcessStrategy;
import com.lottery.system.service.draw.strategy.PrizeStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DrawAppService {

    private static final Logger log = LoggerFactory.getLogger(DrawAppService.class);

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final PrizeRepository prizeRepository;
    private final DrawTicketRepository drawTicketRepository;
    private final DrawValidationService drawValidationService;
    private final DrawAlgorithmService drawAlgorithmService;
    private final PrizeStrategyFactory strategyFactory;
    private final DrawMessageProducer messageProducer;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DrawAppService(UserRepository userRepository,
                          ActivityRepository activityRepository,
                          PrizeRepository prizeRepository,
                          DrawTicketRepository drawTicketRepository,
                          DrawValidationService drawValidationService,
                          DrawAlgorithmService drawAlgorithmService,
                          PrizeStrategyFactory strategyFactory,
                          DrawMessageProducer messageProducer,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
      this.userRepository = userRepository;
      this.activityRepository = activityRepository;
      this.prizeRepository = prizeRepository;
      this.drawTicketRepository = drawTicketRepository;
      this.drawValidationService = drawValidationService;
      this.drawAlgorithmService = drawAlgorithmService;
      this.strategyFactory = strategyFactory;
      this.messageProducer = messageProducer;
      this.redisTemplate = redisTemplate;
      this.objectMapper = objectMapper;
    }

    /**
     * Orchestrates the high-concurrency draw flow for multiple draws.
     * @param userId participating user ID
     * @param activityId target activity ID
     * @param drawCount number of draws to perform
     * @return the created DrawTickets list
     */
    @Transactional
    public List<DrawTicket> performDraw(Long userId, Long activityId, int drawCount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));

        // Step 1: Validate draw count using Redis INCR with batch size
        drawValidationService.validateDrawCount(userId, activity, drawCount);

        try {
            // Fetch all prizes for the activity (using Cache-Aside)
            List<Prize> prizes = getPrizesFromCacheOrDb(activityId);
            if (prizes.isEmpty()) {
                throw new IllegalStateException("No prizes configured for activity: " + activityId);
            }

            List<DrawTicket> tickets = new ArrayList<>();

            for (int i = 0; i < drawCount; i++) {
                // Step 2: Run probability algorithm to pick a candidate prize
                Prize selectedPrize = drawAlgorithmService.pickPrize(prizes);

                // Initialize ticket in DB with status INIT
                String ticketId = UUID.randomUUID().toString();
                DrawTicket ticket = DrawTicket.builder()
                        .ticketId(ticketId)
                        .activityId(activityId)
                        .userId(userId)
                        .status(TicketStatus.INIT)
                        .prizeId(selectedPrize.getId())
                        .build();

                drawTicketRepository.save(ticket);

                // Step 3: Fetch process strategy
                PrizeProcessStrategy strategy = strategyFactory.getStrategy(selectedPrize.getPrizeType());

                // Step 4: Execute strategy. If physical stock is gone, downgrade to "None" prize
                boolean success = strategy.execute(ticket, selectedPrize);

                if (!success) {
                    // Downgrade fallback: find None (Thanks) prize
                    Prize nonePrize = prizes.stream()
                            .filter(p -> p.getPrizeType() == 3)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No None/Thanks prize configured for fallback"));

                    PrizeProcessStrategy noneStrategy = strategyFactory.getStrategy(3);
                    noneStrategy.execute(ticket, nonePrize);

                    // Update ticket directly in database for failed/none draws (no asynchronous processing needed)
                    drawTicketRepository.save(ticket);
                } else {
                    // Save the initialized ticket
                    drawTicketRepository.save(ticket);

                    // Only publish to MQ for virtual prizes here (Physical strategy handles its own MQ + outbox logic)
                    if (selectedPrize.getPrizeType() == 2) {
                        messageProducer.sendDrawMessage(ticketId, userId, activityId, ticket.getPrizeId());
                    }
                }
                tickets.add(ticket);
            }

            return tickets;
        } catch (Exception e) {
            // Rollback Redis draw count counter since the draw attempt failed
            drawValidationService.rollbackDrawCount(userId, activityId, drawCount);
            throw e;
        }
    }

    private List<Prize> getPrizesFromCacheOrDb(Long activityId) {
        String redisKey = "activity:" + activityId + ":prizes";
        String cachedPrizes = redisTemplate.opsForValue().get(redisKey);

        if (cachedPrizes != null) {
            try {
                return objectMapper.readValue(cachedPrizes, new TypeReference<List<Prize>>() {});
            } catch (Exception e) {
                log.error("Failed to deserialize cached prizes list for activity: {}", activityId, e);
            }
        }

        // Cache miss: Load from database
        List<Prize> dbPrizes = prizeRepository.findByActivityId(activityId);
        if (!dbPrizes.isEmpty()) {
            try {
                String prizesJson = objectMapper.writeValueAsString(dbPrizes);
                redisTemplate.opsForValue().set(redisKey, prizesJson);
                
                // Proactively warm up individual stock keys for physical prizes
                for (Prize p : dbPrizes) {
                    if (p.getPrizeType() == 1) {
                        String stockKey = "prize:" + p.getId() + ":stock";
                        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(p.getStock()));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to serialize and warm up prizes list to Redis for activity: {}", activityId, e);
            }
        }
        return dbPrizes;
    }
}
