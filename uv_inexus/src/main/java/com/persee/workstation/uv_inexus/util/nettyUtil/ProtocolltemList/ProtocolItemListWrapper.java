package com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * JAXB XML包装类，用于包装 List<ProtocolItem>
 */
@XmlRootElement(name = "Protocols")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProtocolItemListWrapper {

    @XmlElement(name = "ProtocolItem")
    private List<ProtocolItem> items;

    public List<ProtocolItem> getItems() {
        return items;
    }

    public void setItems(List<ProtocolItem> items) {
        this.items = items;
    }
}

