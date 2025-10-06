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

    const payload = {
        notification: {
            title: `Nuevo mensaje de ${senderName}`,
            body: notificationBody,
        },
        data: {
            senderId: senderId,
        },
    };

    const usersSnapshot = await admin.firestore().collection("users").get();
    const tokens = [];
    usersSnapshot.forEach(doc => {
        if (doc.id !== senderId && doc.data().fcmToken) {
            tokens.push(doc.data().fcmToken);
        }
    });

    if (tokens.length > 0) {
        console.log(`Sending notification to ${tokens.length} tokens.`);
        await admin.messaging().sendToDevice(tokens, payload);
    } else {
        console.log("No tokens found to send notification.");
    }
});