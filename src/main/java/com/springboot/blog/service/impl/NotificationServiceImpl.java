package com.springboot.blog.service.impl;

import com.springboot.blog.aspect.GetExecutionTime;
import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.payload.PostDto;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.NotificationService;
import com.springboot.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final UserService userService;
    private final RestTemplate restTemplate;
    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;
    /**
     * Sends weekly personalized post recommendations via email to users at a fixed rate.
     * The execution of this method is automatically handled by @EnableScheduling at root of the application level.
     */
    @Override
    @GetExecutionTime
    @Scheduled(fixedRate = 7 * 24 * 60 * 60 * 1000)
    public void sendRecommendedPostNotifications() {
        Set<UserEntity> userList = userRepository.findAllUsers();

        for (UserEntity user : userList) {
            Set<PostDto> recommendedPosts = userService.getRecommendedPosts(user.getId());

            if (!recommendedPosts.isEmpty()) {
                String message = generateHtmlEmailContent(recommendedPosts);
                sendEmail(user.getEmail(), user.getName() + ", Your Customized Weekly Roundup is Here!", message);
            }
        }
    }
    /**
     * Generates HTML content for the recommended post email.
     *
     * @param recommendedPosts Set of recommended posts for a user.
     * @return HTML content for the email.
     */
    private String generateHtmlEmailContent(Set<PostDto> recommendedPosts) {
        // Amazing and fancy HTML template for the email with dynamically generated images
        String htmlTemplate = "<html>" +
                "<head>" +
                "   <style>" +
                "       body {" +
                "           font-family: 'Arial', sans-serif;" +
                "           background-color: #f5f5f5;" +
                "           color: #333;" +
                "           text-align: center;" +
                "       }" +
                "       h2 {" +
                "           color: #0066cc;" +
                "       }" +
                "       ul {" +
                "           list-style-type: none;" +
                "           padding: 0;" +
                "       }" +
                "       li {" +
                "           background-color: #fff;" +
                "           padding: 10px;" +
                "           margin: 5px 0;" +
                "           border-radius: 5px;" +
                "       }" +
                "       img {" +
                "           width: 100%;" +
                "           max-width: 500px;" +
                "           border-radius: 10px;" +
                "           margin-top: 20px;" +
                "       }" +
                "   </style>" +
                "</head>" +
                "<body>" +
                "<h2>Discover Exciting Content!</h2>" +
                "<p>Check out these recommended posts:</p>" +
                "<ul>";

        // adding recommended posts to the HTML
        for (PostDto post : recommendedPosts) {
            // fetch a unique designer image for each post
            String imageUrl = getRandomImageUrl();

            htmlTemplate += "<li>" +
                    "<strong>" + post.getTitle() + "</strong>" +
                    "<img src='" + imageUrl + "' alt='Random Image'>" +
                    "</li>";
        }
        // close the HTML tags for the email
        htmlTemplate += "</ul></body></html>";
        return htmlTemplate;
    }

    private void sendEmail(String to, String subject, String body) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
        try {
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // set to true to indicate HTML content
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Unable to send email to the user!");
        }
    }

    private String getRandomImageUrl() {
        // call the external API to get a designer image reference in a form of URL
        return restTemplate.getForObject("https://random.responsiveimages.io/v2/unsplash", String.class);
    }
}