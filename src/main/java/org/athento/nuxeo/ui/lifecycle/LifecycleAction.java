package org.athento.nuxeo.ui.lifecycle;

/**
 * Lifecycle actions.
 */
public interface LifecycleAction {

    /**
     * Execute the current transition.
     *
     */
    void followTransition();

}
