const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Triggers when a new CR announcement is created in Realtime Database.
 * Sends an FCM push notification to the corresponding section topic.
 */
exports.sendCrAnnouncementNotification = functions.database
  .ref("/voting_controls/{dept}/{sec}/announcements/{noticeId}")
  .onCreate(async (snapshot, context) => {
    const notice = snapshot.val();
    if (!notice) return null;

    const dept = context.params.dept;
    const sec = context.params.sec;

    // Sanitize topic to match [a-zA-Z0-9-_.~%]+
    const sanitizedDept = dept.replace(/[^a-zA-Z0-9-_.~%]/g, "_");
    const sanitizedSec = sec.replace(/[^a-zA-Z0-9-_.~%]/g, "_");
    const topic = `cr_notice_${sanitizedDept}_${sanitizedSec}`;

    const title = notice.title ? `CR Notice: ${notice.title}` : "New CR Notice";
    const body = notice.body || "A new announcement has been posted for your section.";
    const postedBy = notice.postedBy || "CR";

    const message = {
      topic: topic,
      notification: {
        title: title,
        body: body,
      },
      data: {
        title: title,
        body: body,
        postedBy: postedBy,
        timestamp: String(notice.timestamp || Date.now()),
        dept: dept,
        sec: sec,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "crconnect_cr_notices",
          sound: "default",
          priority: "high",
          defaultSound: true,
          defaultVibrateTimings: true,
        },
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log(`Successfully sent message to topic ${topic}:`, response);
      return response;
    } catch (error) {
      console.error(`Error sending message to topic ${topic}:`, error);
      return null;
    }
  });

/**
 * Global Announcements trigger (optional)
 */
exports.sendGlobalAnnouncementNotification = functions.database
  .ref("/announcements/{noticeId}")
  .onCreate(async (snapshot, context) => {
    const notice = snapshot.val();
    if (!notice) return null;

    const topic = "cr_notice_all";
    const title = notice.title ? `Announcement: ${notice.title}` : "University Announcement";
    const body = notice.body || "A new university announcement has been posted.";

    const message = {
      topic: topic,
      notification: {
        title: title,
        body: body,
      },
      data: {
        title: title,
        body: body,
        timestamp: String(notice.timestamp || Date.now()),
      },
      android: {
        priority: "high",
        notification: {
          channelId: "crconnect_cr_notices",
          sound: "default",
          priority: "high",
        },
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("Successfully sent global announcement:", response);
      return response;
    } catch (error) {
      console.error("Error sending global announcement:", error);
      return null;
    }
  });
