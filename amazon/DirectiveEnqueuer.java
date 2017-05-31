package com.goertek.smartear.amazon;

import android.util.Log;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveEnqueuer implements MultipartParserConsumer {

    private static final boolean D = false;
    private static final String TAG = DirectiveEnqueuer.class.getSimpleName();

    // The authority for the current dialogRequestId.
    private final DialogRequestIdAuthority dialogRequestIdAuthority;

    // Queue made up of all dependent directives for the current dialogRequestId
    private final Queue<Directive> dependentQueue;

    // Queue made up of all directives without a dialogRequestId
    private final Queue<Directive> independentQueue;

    // Queue for incomplete directives. A directive is incomplete if it still needs some attached
    // content to be associated with it.
    private final Queue<Directive> incompleteDirectiveQueue;

    // Map of all attachments which have not yet been matched with directives.
    private final Map<String, InputStream> attachments;

    public DirectiveEnqueuer(DialogRequestIdAuthority dialogRequestIdAuthority,
                             Queue<Directive> dependentQueue, Queue<Directive> independentQueue) {
        this.dialogRequestIdAuthority = dialogRequestIdAuthority;
        this.dependentQueue = dependentQueue;
        this.independentQueue = independentQueue;
        incompleteDirectiveQueue = new LinkedList<>();
        attachments = new HashMap<>();
    }

    @Override
    public synchronized void onDirective(Directive directive) {
        incompleteDirectiveQueue.add(directive);
        matchAttachmentsWithDirectives();
    }

    @Override
    public synchronized void onDirectiveAttachment(String contentId,
                                                   InputStream attachmentContent) {

        if(D) Log.d(TAG,"onDirectiveAttachment,,,");
        attachments.put(contentId, attachmentContent);

        matchAttachmentsWithDirectives();

    }

    private void matchAttachmentsWithDirectives() {

        for (Directive directive : incompleteDirectiveQueue) {
            Payload payload = directive.getPayload();
            if (payload instanceof AttachedContentPayload) {
                AttachedContentPayload attachedContentPayload = (AttachedContentPayload) payload;
                String contentId = attachedContentPayload.getAttachedContentId();

                if(D) Log.d(TAG,"contentId is:\n"+contentId);

                InputStream attachment = attachments.remove(contentId);

                if (attachment != null) {
                    attachedContentPayload.setAttachedContent(contentId, attachment);
                }else {
                    if(D) Log.d(TAG,"no inputstream in attachment...");
                }
            }
        }

        findCompleteDirectives();
    }

    private void findCompleteDirectives() {
        if(D) Log.d(TAG,"findCompleteDirectives...");
        Iterator<Directive> iterator = incompleteDirectiveQueue.iterator();
        while (iterator.hasNext()) {
            Directive directive = iterator.next();
            Payload payload = directive.getPayload();
            if (payload instanceof AttachedContentPayload) {

                if(D) Log.d(TAG,"audio attachment...");
                AttachedContentPayload attachedContentPayload = (AttachedContentPayload) payload;

                if (attachedContentPayload.hasAttachedContent()) {
                    // The front most directive IS complete.
                    enqueueDirective(directive);
                    iterator.remove();
                }else {
                    if(D) Log.d(TAG,"no enqueue...");
                    break;
                }
            }else {

                if(D) Log.d(TAG,"no audio attachment...");
                // Immediately enqueue any directive which does not contain audio content
                enqueueDirective(directive);
                iterator.remove();
            }
        }
    }

    private void enqueueDirective(Directive directive) {

        if(D) Log.d(TAG,"enqueueDirective...");
        String dialogRequestId = directive.getDialogRequestId();
        if (dialogRequestId == null) {
            independentQueue.add(directive);
        } else if (dialogRequestIdAuthority.isCurrentDialogRequestId(dialogRequestId)) {
            dependentQueue.add(directive);
        }
    }
}
