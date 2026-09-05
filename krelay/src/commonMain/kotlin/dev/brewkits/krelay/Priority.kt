package dev.brewkits.krelay

/**
 * Action priority levels for queue management.
 *
 * When multiple actions are queued, higher priority actions
 * will be executed first during replay.
 */
enum class ActionPriority(val value: Int) {
    /**
     * Low priority - executed last.
     * Use for: Analytics, logging, non-critical updates
     */
    LOW(0),

    /**
     * Normal priority - default.
     * Use for: Regular UI updates, standard notifications
     */
    NORMAL(50),

    /**
     * High priority - executed before normal.
     * Use for: Important notifications, navigation commands
     */
    HIGH(100),

    /**
     * Critical priority - executed first.
     * Use for: Error dialogs, critical user feedback, security alerts
     */
    CRITICAL(1000);

    companion object {
        /**
         * Default priority for all actions.
         */
        val DEFAULT = NORMAL
    }
}

