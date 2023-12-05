package com.springboot.blog.filters;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.blog.exception.BlogAPIException;
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
public class RegistrationValidationFilter implements Filter {

    private final UserRepository userRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Check if the request URL matches the registration endpoint
        if (isRegistrationEndpoint(request.getRequestURI())) {
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
            RegisterDTO registerDTO = objectMapper.readValue(requestBody, RegisterDTO.class);

            String name = registerDTO.getName();
            String email = registerDTO.getEmail();
            String username = registerDTO.getUsername();
            String password = registerDTO.getPassword();
            Set<String> favGenres = registerDTO.getFavGenres();

            try {
                // validate input before allowing the request to proceed
                if (!isNameValid(name)) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("the provided name is not of valid format.");
                }
                if (!isValidEmail(email)) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("email already exists or is not of valid format.");
                }
                if (!isValidUsername(username)) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("username already exists or is not of valid format.");
                }

                if (!isStrongPassword(password)) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("password is not of valid format or is too weak.");
                }
                if (favGenres == null || favGenres.isEmpty()) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    throw new BlogAPIException("please add favGenres to your request body.");
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

    private boolean isRegistrationEndpoint(String requestURI) {
        return "/api/auth/register".equals(requestURI) || "/api/auth/signup".equals(requestURI);
    }

    private boolean isNameValid(String name) {
        return StringUtils.hasText(name) && name.length() >= 4 && name.matches("^[A-Za-z]+$");
    }

    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && !userRepository.findByEmail(email).isPresent() && email.contains("@");
    }

    private boolean isValidUsername(String username) {
        return StringUtils.hasText(username) && !userRepository.findByUsername(username).isPresent() &&
                Pattern.compile("[a-zA-Z0-9]+").matcher(username).matches();
    }

    private boolean isStrongPassword(String password) {
        return StringUtils.hasText(password) && password.length() >= 4 && password.matches("^(?=.*[A-Za-z])([A-Za-z0-9]+)$");
    }

}