package com.lottery.system.interceptor;

import com.lottery.system.entity.User;
import com.lottery.system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RateLimitInterceptorTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private UserRepository userRepository;
    private RateLimitInterceptor interceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        userRepository = mock(UserRepository.class);
        interceptor = new RateLimitInterceptor(redisTemplate, userRepository);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = new Object();

        // Setup mock user
        User user = User.builder().id(99L).username("testuser").build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Setup mock security context
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testPreHandle_UnderLimit_AllowsRequest() throws Exception {
        String rateKey = "rate:limit:99";
        // Mock Redis increment: returns 2 (under limit of 3)
        when(valueOperations.increment(rateKey, 1)).thenReturn(2L);

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    public void testPreHandle_ExceedLimit_Returns429AndFalse() throws Exception {
        String rateKey = "rate:limit:99";
        // Mock Redis increment: returns 4 (exceeds limit of 3)
        when(valueOperations.increment(rateKey, 1)).thenReturn(4L);

        // Mock writer to prevent null pointer
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        boolean result = interceptor.preHandle(request, response, handler);

        assertFalse(result);
        verify(response, times(1)).setStatus(429);
        assertTrue(stringWriter.toString().contains("Too many requests"));
    }
}
