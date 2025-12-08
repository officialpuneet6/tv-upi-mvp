document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Firebase (handled by /__/firebase/init.js)
    try {
        const app = firebase.app();
        const db = firebase.firestore();

        console.log("Firebase initialized");

        setupListener(db);
    } catch (e) {
        console.error("Firebase initialization failed. Are you running via 'firebase serve'?", e);
        // Fallback or Alert
        alert("Error: Firebase not initialized. Make sure to run this via Firebase Hosting.");
    }
});

let audioEnabled = false;

// 2. Audio Context (TTS needs user interaction first usually)
window.startAudio = function () {
    audioEnabled = true;
    const idleScreen = document.getElementById('idle-screen');
    const msg = idleScreen.querySelector('p:last-child');
    if (msg) msg.textContent = "Audio Enabled ✅";

    // Test speak silently to unlock
    const utterance = new SpeechSynthesisUtterance("");
    window.speechSynthesis.speak(utterance);
}

function setupListener(db) {
    const notificationsRef = db.collection('tv_notifications');

    // Listen for the most recent notification
    notificationsRef.orderBy('createdAt', 'desc').limit(1)
        .onSnapshot((snapshot) => {
            snapshot.docChanges().forEach((change) => {
                if (change.type === "added") {
                    const data = change.doc.data();

                    // Simple check: ignore old events on page load (older than 10 seconds)
                    const now = new Date().getTime();
                    // Firestore timestamp to millis
                    const eventTime = data.createdAt ? data.createdAt.toMillis() : now;

                    if (now - eventTime < 10000) {
                        showNotification(data);
                    } else {
                        console.log("Ignoring old event:", data);
                    }
                }
            });
        }, (error) => {
            console.error("Firestore listener error:", error);
        });
}

let timeoutId = null;

function showNotification(data) {
    const popup = document.getElementById('notification-popup');
    const amountEl = document.getElementById('amount-text');
    const senderEl = document.getElementById('sender-text');

    amountEl.textContent = data.amount;
    senderEl.textContent = data.sender;

    // Show
    popup.classList.add('active');

    // Voice
    speakRequest(data.amount, data.sender);

    // Auto-hide after 5 seconds
    if (timeoutId) clearTimeout(timeoutId);

    const duration = data.closeAfter || 5000;
    timeoutId = setTimeout(() => {
        popup.classList.remove('active');
    }, duration);
}

function speakRequest(amount, sender) {
    if (!window.speechSynthesis) return;

    // Cancel current speaking
    window.speechSynthesis.cancel();

    // "Received rupees {amount} from {sender}"
    const text = `Received rupees ${amount} from ${sender}`;
    const utterance = new SpeechSynthesisUtterance(text);

    // Optional: Select a better voice
    const voices = window.speechSynthesis.getVoices();
    // Try to find a clear English voice (Google US/UK often good)
    const preferredVoice = voices.find(v => v.name.includes('Google US English')) || voices[0];
    if (preferredVoice) utterance.voice = preferredVoice;

    utterance.rate = 1.0;
    utterance.pitch = 1.0;

    window.speechSynthesis.speak(utterance);
}
