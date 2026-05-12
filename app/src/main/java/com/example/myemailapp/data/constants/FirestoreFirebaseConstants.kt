package com.example.myemailapp.data.constants

object FirestoreFirebaseConstants {
    // Collection names
    object Collections {
        const val USERS = "users"
        const val MESSAGES = "messages"
        const val FOLDERS = "folders"
        const val CONTACTS = "contacts"
        const val ALL_MESSAGES = "allMessages"
        const val MESSAGES_METADATA = "messagesMetadata"
    }

    // Field names
    object Field {
        const val NAME = "name"
        const val PARENT_ID = "parentId"
        const val RECIPIENTS = "recipients"
        const val EMAIL_DATE_TIME = "email.dateTime"
    }
}