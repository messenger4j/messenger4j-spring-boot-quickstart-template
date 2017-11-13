package com.github.messenger4j.quickstart.boot;

import static com.github.messenger4j.Messenger.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.Messenger.VERIFY_TOKEN_REQUEST_PARAM_NAME;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.AUDIO;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.FILE;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.IMAGE;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.VIDEO;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.send.senderaction.SenderAction;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final Messenger messenger;

    @Autowired
    public MessengerPlatformCallbackHandler(final Messenger messenger) {
        this.messenger = messenger;
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
            this.messenger.verifyWebhook(mode, verifyToken);
            return ResponseEntity.ok(challenge);
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
            this.messenger.onReceiveEvents(payload, of(signature), event -> {
                if (event.isTextMessageEvent()) {
                    handleTextMessageEvent(event.asTextMessageEvent());
                }
            });
            logger.debug("Processed callback payload successfully");
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (MessengerVerificationException e) {
            logger.warn("Processing of callback payload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private void handleTextMessageEvent(TextMessageEvent event) {
            logger.debug("Received TextMessageEvent: {}", event);

        final String messageId = event.messageId();
        final String messageText = event.text();
        final String senderId = event.senderId();
        final Instant timestamp = event.timestamp();

            logger.info("Received message '{}' with text '{}' from user '{}' at '{}'",
                    messageId, messageText, senderId, timestamp);

            try {
                switch (messageText.toLowerCase()) {
                    case "user":
                        sendUserDetails(senderId);
                        break;

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
                    /*
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
                    */
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
            } catch (MessengerApiException | MessengerIOException | MalformedURLException e) {
                handleSendException(e);
            }
    }

    private void sendUserDetails(String recipientId) throws MessengerApiException, MessengerIOException {
        final UserProfile userProfile = this.messenger.queryUserProfile(recipientId);
        sendTextMessage(recipientId, String.format("Your name is %s and you are %s", userProfile.firstName(), userProfile.gender()));
        logger.info("User Profile Picture: {}", userProfile.profilePicture());
    }

    private void sendImageMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(IMAGE, new URL(RESOURCE_URL + "/assets/rift.png"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendRichMediaMessage(String recipientId, UrlRichMediaAsset richMediaAsset) throws MessengerApiException, MessengerIOException {
        final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, richMediaMessage);
        this.messenger.send(messagePayload);
    }

    private void sendGifMessage(String recipientId) throws MessengerApiException, MessengerIOException,
            MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(IMAGE, new URL("https://media.giphy.com/media/11sBLVxNs7v6WA/giphy.gif"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendAudioMessage(String recipientId) throws MessengerApiException, MessengerIOException,
            MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(AUDIO, new URL(RESOURCE_URL + "/assets/sample.mp3"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendVideoMessage(String recipientId) throws MessengerApiException, MessengerIOException,
            MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(VIDEO, new URL(RESOURCE_URL + "/assets/allofus480.mov"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendFileMessage(String recipientId) throws MessengerApiException, MessengerIOException,
            MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(FILE, new URL(RESOURCE_URL + "/assets/test.txt"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    /*
    private void sendButtonMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        final List<Button> buttons = Button.newListBuilder()
                .addUrlButton("Open Web URL", "https://www.oculus.com/en-us/rift/").toList()
                .addPostbackButton("Trigger Postback", "DEVELOPER_DEFINED_PAYLOAD").toList()
                .addCallButton("Call Phone Number", "+16505551234").toList()
                .build();

        final ButtonTemplate buttonTemplate = ButtonTemplate.newBuilder("Tap a button", buttons).build();
        this.messenger.sendTemplate(recipientId, buttonTemplate);
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

        this.messenger.sendTemplate(recipientId, genericTemplate);
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

        this.messenger.sendTemplate(recipientId, receiptTemplate);
    }

    private void sendQuickReply(String recipientId) throws MessengerApiException, MessengerIOException {
        final List<QuickReply> quickReplies = QuickReply.newListBuilder()
                .addTextQuickReply("Action", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_ACTION").toList()
                .addTextQuickReply("Comedy", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_COMEDY").toList()
                .addTextQuickReply("Drama", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_DRAMA").toList()
                .addLocationQuickReply().toList()
                .build();

        this.messenger.sendTextMessage(recipientId, "What's your favorite movie genre?", quickReplies);
    }

    private void sendReadReceipt(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.sendSenderAction(recipientId, SenderAction.MARK_SEEN);
    }
    */
    private void sendTypingOn(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_ON));
    }

    private void sendTypingOff(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_ON));
    }
    /*
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

    private FallbackEventHandler newFallbackEventHandler() {
        return event -> {
            logger.debug("Received FallbackEvent: {}", event);

            final String senderId = event.getSender().getId();
            logger.info("Received unsupported message from user '{}'", senderId);
        };
    }
    */

    private void sendTextMessage(String recipientId, String text) {
        try {
            final IdRecipient recipient = IdRecipient.create(recipientId);
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload = MessagePayload.create(recipient, textMessage, of(notificationType));
            this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
        }
    }

    private void handleSendException(Exception e) {
        logger.error("Message could not be sent. An unexpected error occurred.", e);
    }
}
