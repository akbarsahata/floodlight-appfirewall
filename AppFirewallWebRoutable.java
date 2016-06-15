package net.floodlightcontroller.appfirewall;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;
import net.floodlightcontroller.appfirewall.AppFirewallResource;
import net.floodlightcontroller.appfirewall.AppFirewallResourceAdd;

public class AppFirewallWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/list", AppFirewallResource.class);
		router.attach("/add", AppFirewallResourceAdd.class);
		router.attach("/delete", AppFirewallResource.class);
		
		return router;
	}

	@Override
	public String basePath() {
		return "/wm/appfirewall";
	}

}
