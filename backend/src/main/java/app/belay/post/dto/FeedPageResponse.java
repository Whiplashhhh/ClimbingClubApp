package app.belay.post.dto;

import java.util.List;

public record FeedPageResponse(List<FeedPostResponse> items, int page, int size, boolean hasNext) {}
