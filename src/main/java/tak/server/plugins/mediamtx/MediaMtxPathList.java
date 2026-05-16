/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/**
 * Page wrapper matching MediaMTX's {@code PathList} schema for
 * {@code /v3/paths/list}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaMtxPathList {

    private long pageCount;
    private long itemCount;
    private List<MediaMtxPath> items = Collections.emptyList();

    public long getPageCount() { return pageCount; }
    public void setPageCount(long pageCount) { this.pageCount = pageCount; }

    public long getItemCount() { return itemCount; }
    public void setItemCount(long itemCount) { this.itemCount = itemCount; }

    public List<MediaMtxPath> getItems() { return items; }
    public void setItems(List<MediaMtxPath> items) {
        this.items = (items != null) ? items : Collections.emptyList();
    }
}
