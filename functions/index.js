const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendChatNotification = onDocumentCreated("messages/{messageId}", async (event) => {
    const snap = event.data;
    if (!snap) {
        console.log("No data associated with the event");
        return;
    }

    const newMessage = snap.data();
    const senderId = newMessage.senderId;

    const userDoc = await admin.firestore().collection("users").doc(senderId).get();
    if (!userDoc.exists) {
        console.log(`Sender with ID ${senderId} not found.`);
        return;
    }
    const senderName = userDoc.data().name || "Alguien";

    let notificationBody;
    if (newMessage.imageUrl) {
        notificationBody = `Te ha enviado una imagen.`;
    } else {
        notificationBody = newMessage.text;
    }

    const usersSnapshot = await admin.firestore().collection("users").get();
    const tokens = [];
    usersSnapshot.forEach(doc => {
        if (doc.id !== senderId && doc.data().fcmToken) {
            tokens.push(doc.data().fcmToken);
        }
    });

    if (tokens.length > 0) {
        console.log(`Sending notification to ${tokens.length} tokens using sendEachForMulticast.`);
        
        const message = {
            notification: {
                title: `Nuevo mensaje de ${senderName}`,
                body: notificationBody,
            },
            tokens: tokens, // 'tokens' en plural para sendEachForMulticast
        };

        try {
            const response = await admin.messaging().sendEachForMulticast(message);
            console.log("Successfully sent message:", response);
            if (response.failureCount > 0) {
                const failedTokens = [];
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        failedTokens.push(tokens[idx]);
                    }
                });
                console.log("List of tokens that caused failures: " + failedTokens);
            }
        } catch (error) {
            console.log("Error sending message:", error);
        }

    } else {
        console.log("No tokens found to send notification.");
    }
});
