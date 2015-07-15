package view.interlocutors;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import controller.ManagerBridge;

public class RemoveItemFavorites extends AbstractInterlocutor {

	public RemoveItemFavorites() {
		super();
	}

	@Override
	public void run() {
		if(!isInitialized()) return;
		try {
			JSONObject c = getJSON(content);
			String itemKey = c.getString("itemKey");
			ManagerBridge.removeFavoriteItem(itemKey);
			JSONObject data = new JSONObject();
			data.put("query", "itemFavoritesRemoved");
			JSONObject content = new JSONObject();
			content.put("itemKey", itemKey);
			data.put("content", content);
			com.sendText(data.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			this.reset();
		}
	}

}