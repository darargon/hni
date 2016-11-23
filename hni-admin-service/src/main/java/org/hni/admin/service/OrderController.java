package org.hni.admin.service;

import java.time.LocalDate;
import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.hni.common.DateUtils;
import org.hni.order.om.Order;
import org.hni.order.service.OrderService;
import org.hni.provider.om.Provider;
import org.hni.provider.om.ProviderLocation;
import org.hni.user.om.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.monitorjbl.json.JsonView;
import com.monitorjbl.json.Match;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "/orders", description = "Operations on Orders and OrderItems")
@Component
@Path("/orders")
public class OrderController extends AbstractBaseController {
	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
	
	@Inject private OrderService orderService;
		
	@GET
	@Path("/{id}")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Returns the order with the given id"
		, notes = ""
		, response = Order.class
		, responseContainer = "")
	public String getOrder(@PathParam("id") Long id) {
		return serializeOrderToJson(orderService.get(id));
	}

	@POST
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Creates a new order or saves the order with the given id"
		, notes = "An order without an ID field will be created"
		, response = Order.class
		, responseContainer = "")
	public String saveOrder(Order order) {
		return serializeOrderToJson(orderService.save(order));
	}

	@DELETE
	@Path("/{id}")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Deletes the order with the given id"
		, notes = ""
		, response = Order.class
		, responseContainer = "")
	public String getDelete(@PathParam("id") Long id) {
		return serializeOrderToJson(orderService.delete(new Order(id)));
	}

	@GET
	@Path("/next")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Returns the next order obtaining a lock on it so others cannot work on it at the same time.  You may pass an optional parameter 'providerId' to filter to a specific provider"
		, notes = ""
		, response = Order.class
		, responseContainer = "")
	public String getNextOrder(@QueryParam("providerId") Long providerId) {
		if ( null != providerId ) {
			return serializeOrderToJson(orderService.next(new Provider(providerId)));
		}
		return serializeOrderToJson(orderService.next());
	}

	@DELETE
	@Path("/lock/{id}")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Forcibly, removes a lock, if it exists, from the order"
		, notes = ""
		, response = Order.class
		, responseContainer = "")
	public void removeLock(@PathParam("id") Long id) {
		logger.info("Unlocking Order "+id);
		orderService.releaseLock(orderService.get(id));
	}

	private static final String ORDER_COUNT = "{\"order-count\":\"%d\"}";
	@GET
	@Path("/count")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Returns the # pending orders. You may pass an optional parameter 'providerId' to filter to a specific provider"
		, notes = ""
		, response = Order.class
		, responseContainer = "")
	public String getOrderCount(@QueryParam("providerId") Long providerId) {
		if ( null != providerId ) {
			return String.format(ORDER_COUNT, orderService.countOrders(new Provider(providerId)));
		}
		return String.format(ORDER_COUNT, orderService.countOrders());
	}
	
	
	@GET
	@Path("/users/{id}")
	@Produces({MediaType.APPLICATION_JSON})
	@ApiOperation(value = "Returns a collection of orders for the given user between the given dates.  If the endDate is not supplied it will default to current date"
	, notes = "accepted date formats yyyy-mm-dd, yyyy/mm/dd, mm-dd-yyyy, mm/dd/yyyy"
	, response = Order.class
	, responseContainer = "")
	public Collection<Order> getUserOrdersBetweenDates(@PathParam("id") Long id, @QueryParam("startDate") String startDate , @QueryParam("endDate") String endDate) {
		
		LocalDate start = DateUtils.parseDate(startDate);
		LocalDate end = LocalDate.now();
		if ( !StringUtils.isEmpty(endDate) ) {
			end = DateUtils.parseDate(endDate);
		}
		return orderService.get(new User(id), start, end);
	}

	private String serializeOrderToJson(Order order) {
		try {
			String json = mapper.writeValueAsString(JsonView.with(order)
					.onClass(User.class, Match.match().exclude("*").include("id", "firstName", "lastName"))
					.onClass(ProviderLocation.class, Match.match().include("*").exclude("created", "createdById"))
					.onClass(Provider.class, Match.match().include("*").exclude("created", "createdById")));
			return json;
		} catch (JsonProcessingException e) {
			logger.error("Serializing User object:"+e.getMessage(), e);
		}
		return "{}";
	}
}
