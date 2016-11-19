package org.hni.order.service;

import org.hni.order.dao.DefaultPartialOrderDAO;
import org.hni.order.dao.OrderDAO;
import org.hni.order.om.Order;
import org.hni.order.om.OrderItem;
import org.hni.order.om.PartialOrder;
import org.hni.order.om.TransactionPhase;
import org.hni.provider.om.MenuItem;
import org.hni.provider.om.ProviderLocation;
import org.hni.provider.service.GeoCodingService;
import org.hni.user.dao.UserDAO;
import org.hni.user.om.Address;
import org.hni.user.om.User;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class DefaultOrderProcessor implements OrderProcessor {
    private static final double PROVIDER_SEARCH_RADIUS = 5;

    @Inject
    private UserDAO userDao;

    @Inject
    DefaultPartialOrderDAO partialOrderDAO;

    @Inject
    OrderDAO orderDAO;

    @Inject
    private GeoCodingService geoService;


    public String processMessage(User user, String message) {
        //this partial order is the one I get for this user
        PartialOrder order = partialOrderDAO.get(user);
        if (order == null) {
            order = new PartialOrder();
            order.setTransactionPhase(TransactionPhase.MEAL);
        }

        TransactionPhase phase = order.getTransactionPhase();
        String output = "";

        switch (phase) {
            case MEAL:
                output = requestingMeal(message, order);
                break;
            case PROVIDING_ADDRESS:
                output = findNearbyMeals(message, order);
                break;
            case CHOOSING_LOCATION:
                output = chooseLocation(message, order);
                break;
            case CHOOSING_MENU_ITEM:
                //this is chosen w/ provider for now
                break;
            case CONFIRM_OR_CONTINUE:
                output = confirmOrContinueOrder(message, order);
                break;
            default:
                //shouldn't get here
        }
        partialOrderDAO.save(order);
        return output;
    }

    public String processMessage(Long userId, String message) {
        return processMessage(userDao.get(userId), message);
    }

    private String requestingMeal(String request, PartialOrder order) {
        order.setTransactionPhase(TransactionPhase.PROVIDING_ADDRESS);
        return "Please provide your address";
    }

    private String findNearbyMeals(String addressString, PartialOrder order) {
        String output = "";
        Optional<Address> address = geoService.resolveAddress(addressString);
        if (address.isPresent()) {
            //TODO actually find nearby addresses
            // List<ProviderLocation> nearbyProviders = geoService.resolveAddress(address.get(), PROVIDER_SEARCH_RADIUS);
            List<ProviderLocation> nearbyProviders = new ArrayList<>();
            order.setProviderLocationsForSelection(nearbyProviders);
            List<MenuItem> items = new ArrayList<>();
            for (ProviderLocation location : nearbyProviders) {
                //TODO get the currently available menu items, not just first
                items.add(location.getProvider().getMenus().iterator().next().getMenuItems().iterator().next());
            }
            order.setMenuItemsForSelection(items);
            for (int i = 0 ; i < 3; i++) {
                output += i + ") " + nearbyProviders.get(i).getName() + "(" + items.get(i).getName() + ")\n";
            }
            order.setTransactionPhase(TransactionPhase.CHOOSING_LOCATION);
        } else {
            output = "Invalid address, please try again";
        }
        return output;
    }


    private String chooseLocation(String message, PartialOrder order) {
        String output = "";
        try {
            int index = Integer.parseInt(message);
            if (index < 1 || index > 3) {
                throw new IndexOutOfBoundsException();
            }
            ProviderLocation location = order.getProviderLocationsForSelection().get(index-1);
            order.setChosenProvider(location);
            MenuItem chosenItem = order.getMenuItemsForSelection().get(index-1);
            order.getOrderItems().add(new OrderItem((long)1, chosenItem.getPrice(), chosenItem));
            order.setTransactionPhase(TransactionPhase.CONFIRM_OR_CONTINUE);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            output = "Please provide a number between 1-3";
        }
        return output;
    }

    private String confirmOrContinueOrder(String message, PartialOrder order) {
        String output = "";
        switch (message.toUpperCase()) {
            case "CONFIRM":
                //TODO create new completed order
                Order finalOrder = new Order();
                finalOrder.setUserId(order.getUser().getId());
                finalOrder.setOrderDate(new Date());
                finalOrder.setProviderLocation(order.getChosenProvider());
                finalOrder.setOrderItems(order.getOrderItems());
                finalOrder.setSubTotal(order.getOrderItems().stream().map(item -> (item.getAmount() * item.getQuantity())).reduce(0.0, Double::sum));
                orderDAO.save(finalOrder);
                break;
            case "CONTINUE":
                order.setTransactionPhase(TransactionPhase.CHOOSING_LOCATION);
                break;
            default:
                output = "Please respond with CONFIRM or CONTINUE";
        }
        return output;
    }

}