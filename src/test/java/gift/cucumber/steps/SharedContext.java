package gift.cucumber.steps;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
public class SharedContext {

	private Response lastResponse;
	private long savedCategoryId;

	public Response getLastResponse() {
		return lastResponse;
	}

	public void setLastResponse(Response response) {
		this.lastResponse = response;
	}

	public long getSavedCategoryId() {
		return savedCategoryId;
	}

	public void setSavedCategoryId(long categoryId) {
		this.savedCategoryId = categoryId;
	}
}
