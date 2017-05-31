package com.goertek.smartear.amazon;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class Event extends Message{
    public Event(Header header,Payload payload) {
        super(header,payload, StringUtils.EMPTY);
    }
}

