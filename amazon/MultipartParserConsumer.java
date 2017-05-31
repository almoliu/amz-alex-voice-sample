package com.goertek.smartear.amazon;

import java.io.InputStream;

/**
 * Created by almo.liu on 2016/11/17.
 */

public interface MultipartParserConsumer {
    void onDirective(Directive directive);
    void onDirectiveAttachment(String contentId, InputStream attachmentContent);
}
