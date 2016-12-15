package com.github.messenger4j.quickstart.boot;

import static com.github.messenger4j.MessengerPlatform.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.MessengerPlatform.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.MessengerPlatform.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.MessengerPlatform.VERIFY_TOKEN_REQUEST_PARAM_NAME;

import com.github.messenger4j.MessengerPlatform;
import com.github.messenger4j.exceptions.MessengerApiException;
import com.github.messenger4j.exceptions.MessengerIOException;
import com.github.messenger4j.exceptions.MessengerVerificationException;
import com.github.messenger4j.receive.MessengerReceiveClient;
import com.github.messenger4j.receive.events.AccountLinkingEvent.AccountLinkingStatus;
import com.github.messenger4j.receive.events.AttachmentMessageEvent.Attachment;
import com.github.messenger4j.receive.events.AttachmentMessageEvent.AttachmentType;
import com.github.messenger4j.receive.events.AttachmentMessageEvent.Payload;
import com.github.messenger4j.receive.handlers.AccountLinkingEventHandler;
import com.github.messenger4j.receive.handlers.AttachmentMessageEventHandler;
import com.github.messenger4j.receive.handlers.EchoMessageEventHandler;
import com.github.messenger4j.receive.handlers.FallbackEventHandler;
import com.github.messenger4j.receive.handlers.MessageDeliveredEventHandler;
import com.github.messenger4j.receive.handlers.MessageReadEventHandler;
import com.github.messenger4j.receive.handlers.OptInEventHandler;
import com.github.messenger4j.receive.handlers.PostbackEventHandler;
import com.github.messenger4j.receive.handlers.QuickReplyMessageEventHandler;
import com.github.messenger4j.receive.handlers.TextMessageEventHandler;
import com.github.messenger4j.send.MessengerSendClient;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.QuickReply;
import com.github.messenger4j.send.Recipient;
import com.github.messenger4j.send.SenderAction;
import com.github.messenger4j.send.buttons.Button;
import com.github.messenger4j.send.templates.ButtonTemplate;
import com.github.messenger4j.send.templates.GenericTemplate;
import com.github.messenger4j.send.templates.ReceiptTemplate;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is the main class for inbound and outbound communication with the Facebook Messenger Platform.
 * The callback handler is responsible for the webhook verification and processing of the inbound messages and events.
 * It showcases the features of the Messenger Platform.
 *
 * @author Max Grabenhorst
 */
@RestController
@RequestMapping("/callback")
public class MessengerPlatformCallbackHandler {

    private static final String RESOURCE_URL =
            "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public";

    private static final Logger logger = LoggerFactory.getLogger(MessengerPlatformCallbackHandler.class);

    private final MessengerReceiveClient receiveClient;
    private final MessengerSendClient sendClient;

    /**
     * Constructs the {@code MessengerPlatformCallbackHandler} and initializes the {@code MessengerReceiveClient}.
     *
     * @param appSecret   the {@code Application Secret}
     * @param verifyToken the {@code Verification Token} that has been provided by you during the setup of the {@code
     *                    Webhook}
     * @param sendClient  the initialized {@code MessengerSendClient}
     */
    @Autowired
    public MessengerPlatformCallbackHandler(@Value("${messenger4j.appSecret}") final String appSecret,
                                            @Value("${messenger4j.verifyToken}") final String verifyToken,
                                            final MessengerSendClient sendClient) {

        logger.debug("Initializing MessengerReceiveClient - appSecret: {} | verifyToken: {}", appSecret, verifyToken);
        this.receiveClient = MessengerPlatform.newReceiveClientBuilder(appSecret, verifyToken)
                .onTextMessageEvent(newTextMessageEventHandler())
                .onAttachmentMessageEvent(newAttachmentMessageEventHandler())
                .onQuickReplyMessageEvent(newQuickReplyMessageEventHandler())
                .onPostbackEvent(newPostbackEventHandler())
                .onAccountLinkingEvent(newAccountLinkingEventHandler())
                .onOptInEvent(newOptInEventHandler())
                .onEchoMessageEvent(newEchoMessageEventHandler())
                .onMessageDeliveredEvent(newMessageDeliveredEventHandler())
                .onMessageReadEvent(newMessageReadEventHandler())
                .fallbackEventHandler(newFallbackEventHandler())
                .build();
        this.sendClient = sendClient;
    }

    /**
     * Webhook verification endpoint.
     *
     * The passed verification token (as query parameter) must match the configured verification token.
     * In case this is true, the passed challenge string must be returned by this endpoint.
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> verifyWebhook(@RequestParam(MODE_REQUEST_PARAM_NAME) final String mode,
                                                @RequestParam(VERIFY_TOKEN_REQUEST_PARAM_NAME) final String verifyToken,
                                                @RequestParam(CHALLENGE_REQUEST_PARAM_NAME) final String challenge) {

        logger.debug("Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}", mode,
                verifyToken, challenge);
        try {
            return ResponseEntity.ok(this.receiveClient.verifyWebhook(mode, verifyToken, challenge));
        } catch (MessengerVerificationException e) {
            logger.warn("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Callback endpoint responsible for processing the inbound messages and events.
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> handleCallback(@RequestBody final String payload,
                                               @RequestHeader(SIGNATURE_HEADER_NAME) final String signature) {

        logger.debug("Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
        try {
            this.receiveClient.processCallbackPayload(payload, signature);
            logger.debug("Processed callback payload successfully");
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (MessengerVerificationException e) {
            logger.warn("Processing of callback payload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private TextMessageEventHandler newTextMessageEventHandler() {
        return event -> {
            logger.debug("Received TextMessageEvent: {}", event);

            final String messageId = event.getMid();
            final String messageText = event.getText();
            final String senderId = event.getSender().getId();
            final Date timestamp = event.getTimestamp();

            logger.info("Received message '{}' with text '{}' from user '{}' at '{}'",
                    messageId, messageText, senderId, timestamp);

            try {
                switch (messageText.toLowerCase()) {
                    case "image":
                        sendImageMessage(senderId);
                        break;

                    case "gif":
                        sendGifMessage(senderId);
                        break;

                    case "audio":
                        sendAudioMessage(senderId);
                        break;

                    case "video":
                        sendVideoMessage(senderId);
                        break;

                    case "file":
                        sendFileMessage(senderId);
                        break;

                    case "button":
                        sendButtonMessage(senderId);
                        break;

                    case "generic":
                        sendGenericMessage(senderId);
                        break;

                    case "receipt":
                        sendReceiptMessage(senderId);
                        break;

                    case "quick reply":
                        sendQuickReply(senderId);
                        break;

                    case "read receipt":
                        sendReadReceipt(senderId);
                        break;

                    case "typing on":
                        sendTypingOn(senderId);
                        break;

                    case "typing off":
                        sendTypingOff(senderId);
                        break;

                    /*
                    case "account linking":
                        sendAccountLinking(senderId);
                        break;
                    */

                    default:
                        sendTextMessage(senderId, messageText);
                }
            } catch (MessengerApiException | MessengerIOException e) {
                handleSendException(e);
            }
        };
    }

    private void sendImageMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendImageAttachment(recipientId, RESOURCE_URL + "/assets/rift.png");
    }

    private void sendGifMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendImageAttachment(recipientId, "https://media.giphy.com/media/11sBLVxNs7v6WA/giphy.gif");
    }

    private void sendAudioMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendAudioAttachment(recipientId, RESOURCE_URL + "/assets/sample.mp3");
    }

    private void sendVideoMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendVideoAttachment(recipientId, RESOURCE_URL + "/assets/allofus480.mov");
    }

    private void sendFileMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendFileAttachment(recipientId, RESOURCE_URL + "/assets/test.txt");
    }

    private void sendButtonMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        final List<Button> buttons = Button.newListBuilder()
                .addUrlButton("Open Web URL", "https://www.oculus.com/en-us/rift/").toList()
                .addPostbackButton("Trigger Postback", "DEVELOPER_DEFINED_PAYLOAD").toList()
                .addCallButton("Call Phone Number", "+16505551234").toList()
                .build();

        final ButtonTemplate buttonTemplate = ButtonTemplate.newBuilder("Tap a button", buttons).build();
        this.sendClient.sendTemplate(recipientId, buttonTemplate);
    }

    private void sendGenericMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        final List<Button> riftButtons = Button.newListBuilder()
                .addUrlButton("Open Web URL", "https://www.oculus.com/en-us/rift/").toList()
                .addPostbackButton("Call Postback", "Payload for first bubble").toList()
                .build();

        final List<Button> touchButtons = Button.newListBuilder()
                .addUrlButton("Open Web URL", "https://www.oculus.com/en-us/touch/").toList()
                .addPostbackButton("Call Postback", "Payload for second bubble").toList()
                .build();


        final GenericTemplate genericTemplate = GenericTemplate.newBuilder()
                .addElements()
                    .addElement("rift")
                        .subtitle("Next-generation virtual reality")
                        .itemUrl("https://www.oculus.com/en-us/rift/")
                        .imageUrl(RESOURCE_URL + "/assets/rift.png")
                        .buttons(riftButtons)
                        .toList()
                    .addElement("touch")
                        .subtitle("Your Hands, Now in VR")
                        .itemUrl("https://www.oculus.com/en-us/touch/")
                        .imageUrl(RESOURCE_URL + "/assets/touch.png")
                        .buttons(touchButtons)
                        .toList()
                    .done()
                .build();

        this.sendClient.sendTemplate(recipientId, genericTemplate);
    }

    private void sendReceiptMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        final String uniqueReceiptId = "order-" + Math.floor(Math.random() * 1000);

        final ReceiptTemplate receiptTemplate = ReceiptTemplate.newBuilder("Peter Chang", uniqueReceiptId, "USD", "Visa 1234")
                .timestamp(1428444852L)
                .addElements()
                    .addElement("Oculus Rift", 599.00f)
                        .subtitle("Includes: headset, sensor, remote")
                        .quantity(1)
                        .currency("USD")
                        .imageUrl(RESOURCE_URL + "/assets/riftsq.png")
                        .toList()
                    .addElement("Samsung Gear VR", 99.99f)
                        .subtitle("Frost White")
                        .quantity(1)
                        .currency("USD")
                        .imageUrl(RESOURCE_URL + "/assets/gearvrsq.png")
                        .toList()
                    .done()
                .addAddress("1 Hacker Way", "Menlo Park", "94025", "CA", "US").done()
                .addSummary(626.66f)
                    .subtotal(698.99f)
                    .shippingCost(20.00f)
                    .totalTax(57.67f)
                    .done()
                .addAdjustments()
                    .addAdjustment().name("New Customer Discount").amount(-50f).toList()
                    .addAdjustment().name("$100 Off Coupon").amount(-100f).toList()
                    .done()
                .build();

        this.sendClient.sendTemplate(recipientId, receiptTemplate);
    }

    private void sendQuickReply(String recipientId) throws MessengerApiException, MessengerIOException {
        final List<QuickReply> quickReplies = QuickReply.newListBuilder()
                .addTextQuickReply("Action", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_ACTION").toList()
                .addTextQuickReply("Comedy", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_COMEDY").toList()
                .addTextQuickReply("Drama", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_DRAMA").toList()
                .addLocationQuickReply().toList()
                .build();

        this.sendClient.sendTextMessage(recipientId, "What's your favorite movie genre?", quickReplies);
    }

    private void sendReadReceipt(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendSenderAction(recipientId, SenderAction.MARK_SEEN);
    }

    private void sendTypingOn(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendSenderAction(recipientId, SenderAction.TYPING_ON);
    }

    private void sendTypingOff(String recipientId) throws MessengerApiException, MessengerIOException {
        this.sendClient.sendSenderAction(recipientId, SenderAction.TYPING_OFF);
    }

    private void sendAccountLinking(String recipientId) {
        // supported by messenger4j since 0.7.0
        // sample implementation coming soon
    }

    private AttachmentMessageEventHandler newAttachmentMessageEventHandler() {
        return event -> {
            logger.debug("Received AttachmentMessageEvent: {}", event);

            final String messageId = event.getMid();
            final List<Attachment> attachments = event.getAttachments();
            final String senderId = event.getSender().getId();
            final Date timestamp = event.getTimestamp();

            logger.info("Received message '{}' with attachments from user '{}' at '{}':",
                    messageId, senderId, timestamp);

            attachments.forEach(attachment -> {
                final AttachmentType attachmentType = attachment.getType();
                final Payload payload = attachment.getPayload();

                String payloadAsString = null;
                if (payload.isBinaryPayload()) {
                    payloadAsString = payload.asBinaryPayload().getUrl();
                }
                if (payload.isLocationPayload()) {
                    payloadAsString = payload.asLocationPayload().getCoordinates().toString();
                }

                logger.info("Attachment of type '{}' with payload '{}'", attachmentType, payloadAsString);
            });

            sendTextMessage(senderId, "Message with attachment received");
        };
    }

    private QuickReplyMessageEventHandler newQuickReplyMessageEventHandler() {
        return event -> {
            logger.debug("Received QuickReplyMessageEvent: {}", event);

            final String senderId = event.getSender().getId();
            final String messageId = event.getMid();
            final String quickReplyPayload = event.getQuickReply().getPayload();

            logger.info("Received quick reply for message '{}' with payload '{}'", messageId, quickReplyPayload);

            sendTextMessage(senderId, "Quick reply tapped");
        };
    }

    private PostbackEventHandler newPostbackEventHandler() {
        return event -> {
            logger.debug("Received PostbackEvent: {}", event);

            final String senderId = event.getSender().getId();
            final String recipientId = event.getRecipient().getId();
            final String payload = event.getPayload();
            final Date timestamp = event.getTimestamp();

            logger.info("Received postback for user '{}' and page '{}' with payload '{}' at '{}'",
                    senderId, recipientId, payload, timestamp);

            sendTextMessage(senderId, "Postback called");
        };
    }

    private AccountLinkingEventHandler newAccountLinkingEventHandler() {
        return event -> {
            logger.debug("Received AccountLinkingEvent: {}", event);

            final String senderId = event.getSender().getId();
            final AccountLinkingStatus accountLinkingStatus = event.getStatus();
            final String authorizationCode = event.getAuthorizationCode();

            logger.info("Received account linking event for user '{}' with status '{}' and auth code '{}'",
                    senderId, accountLinkingStatus, authorizationCode);
        };
    }

    private OptInEventHandler newOptInEventHandler() {
        return event -> {
            logger.debug("Received OptInEvent: {}", event);

            final String senderId = event.getSender().getId();
            final String recipientId = event.getRecipient().getId();
            final String passThroughParam = event.getRef();
            final Date timestamp = event.getTimestamp();

            logger.info("Received authentication for user '{}' and page '{}' with pass through param '{}' at '{}'",
                    senderId, recipientId, passThroughParam, timestamp);

            sendTextMessage(senderId, "Authentication successful");
        };
    }

    private EchoMessageEventHandler newEchoMessageEventHandler() {
        return event -> {
            logger.debug("Received EchoMessageEvent: {}", event);

            final String messageId = event.getMid();
            final String recipientId = event.getRecipient().getId();
            final String senderId = event.getSender().getId();
            final Date timestamp = event.getTimestamp();

            logger.info("Received echo for message '{}' that has been sent to recipient '{}' by sender '{}' at '{}'",
                    messageId, recipientId, senderId, timestamp);
        };
    }

    private MessageDeliveredEventHandler newMessageDeliveredEventHandler() {
        return event -> {
            logger.debug("Received MessageDeliveredEvent: {}", event);

            final List<String> messageIds = event.getMids();
            final Date watermark = event.getWatermark();
            final String senderId = event.getSender().getId();

            if (messageIds != null) {
                messageIds.forEach(messageId -> {
                    logger.info("Received delivery confirmation for message '{}'", messageId);
                });
            }

            logger.info("All messages before '{}' were delivered to user '{}'", watermark, senderId);
        };
    }

    private MessageReadEventHandler newMessageReadEventHandler() {
        return event -> {
            logger.debug("Received MessageReadEvent: {}", event);

            final Date watermark = event.getWatermark();
            final String senderId = event.getSender().getId();

            logger.info("All messages before '{}' were read by user '{}'", watermark, senderId);
        };
    }

    /**
     * This handler is called when either the message is unsupported or when the event handler for the actual event type
     * is not registered. In this showcase all event handlers are registered. Hence only in case of an
     * unsupported message the fallback event handler is called.
     */
    private FallbackEventHandler newFallbackEventHandler() {
        return event -> {
            logger.debug("Received FallbackEvent: {}", event);

            final String senderId = event.getSender().getId();
            logger.info("Received unsupported message from user '{}'", senderId);
        };
    }

    private void sendTextMessage(String recipientId, String text) {
        try {
            final Recipient recipient = Recipient.newBuilder().recipientId(recipientId).build();
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            this.sendClient.sendTextMessage(recipient, notificationType, text, metadata);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
        }
    }

    private void handleSendException(Exception e) {
        logger.error("Message could not be sent. An unexpected error occurred.", e);
    }
}
