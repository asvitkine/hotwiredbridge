package hotwiredbridge.wired;

import java.util.List;

public class NewsListEvent extends WiredEvent {
	private List<NewsPost> newsPosts;

	public List<NewsPost> getNewsPosts() {
		return newsPosts;
	}

	public void setNewsPosts(List<NewsPost> newsPosts) {
		this.newsPosts = newsPosts;
	}
}
