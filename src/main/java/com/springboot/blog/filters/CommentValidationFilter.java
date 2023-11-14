package com.springboot.blog.filters;

import java.io.IOException;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.payload.CommentDto;
import com.springboot.blog.payload.RegisterDTO;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.utils.CachedBodyHttpServletRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CommentValidationFilter implements Filter {

    private final UserRepository userRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Check if the request URL matches the registration endpoint
        if (isCommentEndpoint(request.getRequestURI())) {
            // Wrap the request to make it readable multiple times
            CachedBodyHttpServletRequestWrapper wrappedRequest = new CachedBodyHttpServletRequestWrapper(request);

            // Get the request body as a byte array
            byte[] requestBody = wrappedRequest.getBody();

            // Check if the request body is empty
            if (requestBody.length == 0) {
                // Handle the case where the request body is empty
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.getWriter().write("Request body is empty.");
                return;
            }

            // Read the JSON payload using an ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            CommentDto commentDto = objectMapper.readValue(requestBody, CommentDto.class);

            String name = commentDto.getName();
            String email = commentDto.getEmail();
            String msgTxt = commentDto.getBody();

            try {
                // validate input before allowing the request to proceed
                if (!isNameValid(name)) {
                    // handling invalid name exception
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("the provided name is blank or is not of valid format.");
                }
                if (!isEmailValid(email)) {
                    // handling invalid email exception
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("user email is blank or does not exist in the system.");
                }
                if (!isBodyValid(msgTxt)) {
                    // handling invalid username exception
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("the comment body is blank or is not of valid format.");
                } else {
                    // if the input is valid, continue with the request & response chain
                    filterChain.doFilter(wrappedRequest, response);
                }

            } catch (BlogAPIException e) {
                // catch and handle any custom exceptions
                response.getWriter().write(e.getMessage());
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }
        } else {
            // if not desired endpoint, will continue with the filter chain
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

    // Regular expression pattern to match the dynamic part of the comment URL
    private static final String POSTS_COMMENTS_PATTERN = "^/api/posts/\\d+/comments$";

    private static final Pattern POSTS_COMMENTS_REGEX = Pattern.compile(POSTS_COMMENTS_PATTERN);

    private boolean isCommentEndpoint(String requestURI) {
        return POSTS_COMMENTS_REGEX.matcher(requestURI).matches();
    }

    private boolean isNameValid(String name) {
        return StringUtils.hasText(name) && name.length() >= 4 && name.matches("^[A-Za-z]+$");
    }

    private boolean isEmailValid(String email) {
        return StringUtils.hasText(email) && userRepository.findByEmail(email).isPresent() && email.contains("@");
    }

    private boolean isBodyValid(String comment) {
        return StringUtils.hasText(comment) && Pattern.compile("[a-zA-Z0-9]+").matcher(comment).matches();
    }

}