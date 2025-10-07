const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendChatNotification = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    const snap = event.data;
    if (!snap) {
        console.log("No data associated with the event");
        return;
    }

    const chatId = event.params.chatId;
    const newMessage = snap.data();
    const senderId = newMessage.senderId;

    // Obtener el nombre del remitente
    const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
    if (!senderDoc.exists) {
        console.log(`Sender with ID ${senderId} not found.`);
        return;
    }
    const senderName = senderDoc.data().name || "Alguien";

    // Obtener la información del chat
    const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
    if (!chatDoc.exists) {
        console.log(`Chat with ID ${chatId} not found.`);
        return;
    }
    const chatData = chatDoc.data();
    const chatName = chatData.name; // Puede ser el nombre del grupo o nulo
    const userIds = chatData.userIds;

    // Determinar el título de la notificación
    let notificationTitle = senderName;
    // CORRECCIÓN: Usar la comprobación correcta de JavaScript para un string
    if (chatName && chatName.length > 0) { 
        notificationTitle = `${chatName}: ${senderName}`;
    }

    // Determinar el cuerpo de la notificación
    let notificationBody = newMessage.imageUrl ? `Te ha enviado una imagen.` : newMessage.text;

    // Recopilar los tokens de los destinatarios
    const tokens = [];
    for (const userId of userIds) {
        if (userId !== senderId) {
            const userDoc = await admin.firestore().collection("users").doc(userId).get();
            if (userDoc.exists && userDoc.data().fcmToken) {
                tokens.push(userDoc.data().fcmToken);
            }
        }
    }

    if (tokens.length > 0) {
        console.log(`Sending notification to ${tokens.length} tokens.`);

        const message = {
            notification: {
                title: notificationTitle,
                body: notificationBody,
            },
            data: {
                chatId: chatId, // Enviar el ID del chat para poder abrirlo desde la notificación
                senderId: senderId,
            },
            tokens: tokens,
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
