package com.lottery.system.service.draw;

import com.lottery.system.entity.Activity;
import com.lottery.system.entity.DrawTicket;
import com.lottery.system.entity.Prize;
import com.lottery.system.entity.User;
import com.lottery.system.mq.DrawMessageProducer;
import com.lottery.system.repository.ActivityRepository;
import com.lottery.system.repository.DrawTicketRepository;
import com.lottery.system.repository.PrizeRepository;
import com.lottery.system.repository.UserRepository;
import com.lottery.system.service.draw.strategy.PrizeProcessStrategy;
import com.lottery.system.service.draw.strategy.PrizeStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DrawAppService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final PrizeRepository prizeRepository;
    private final DrawTicketRepository drawTicketRepository;
    private final DrawValidationService drawValidationService;
    private final DrawAlgorithmService drawAlgorithmService;
    private final PrizeStrategyFactory strategyFactory;
    private final DrawMessageProducer messageProducer;

    public DrawAppService(UserRepository userRepository,
                          ActivityRepository activityRepository,
                          PrizeRepository prizeRepository,
                          DrawTicketRepository drawTicketRepository,
                          DrawValidationService drawValidationService,
                          DrawAlgorithmService drawAlgorithmService,
                          PrizeStrategyFactory strategyFactory,
                          DrawMessageProducer messageProducer) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.prizeRepository = prizeRepository;
        this.drawTicketRepository = drawTicketRepository;
        this.drawValidationService = drawValidationService;
        this.drawAlgorithmService = drawAlgorithmService;
        this.strategyFactory = strategyFactory;
        this.messageProducer = messageProducer;
    }

    /**
     * Orchesrates the high-concurrency draw flow.
     * @param userId participating user ID
     * @param activityId target activity ID
     * @return the created DrawTicket
     */
    @Transactional
    public DrawTicket performDraw(Long userId, Long activityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));

        // Step 1: Validate draw count using Redis INCR
        drawValidationService.validateDrawCount(userId, activity);

        // Fetch all prizes for the activity
        List<Prize> prizes = prizeRepository.findByActivityId(activityId);
        if (prizes.isEmpty()) {
            // Rollback Redis draw count counter since the activity configuration is invalid
            drawValidationService.rollbackDrawCount(userId, activityId);
            throw new IllegalStateException("No prizes configured for activity: " + activityId);
        }

        // Step 2: Run probability algorithm to pick a candidate prize
        Prize selectedPrize = drawAlgorithmService.pickPrize(prizes);

        // Initialize ticket in DB with status INIT (0)
        String ticketId = UUID.randomUUID().toString();
        DrawTicket ticket = DrawTicket.builder()
                .ticketId(ticketId)
                .activityId(activityId)
                .userId(userId)
                .status(0) // 0: INIT
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

            // Send message to RabbitMQ for asynchronous database updates
            messageProducer.sendDrawMessage(ticketId, userId, activityId, ticket.getPrizeId());
        }

        return ticket;
    }
}
