package com.GetMyGraphicsCard.subscriptionservice.service;

import com.GetMyGraphicsCard.subscriptionservice.dto.SubscriptionItemDto;
import com.GetMyGraphicsCard.subscriptionservice.entity.Subscription;
import com.GetMyGraphicsCard.subscriptionservice.entity.SubscriptionItem;
import com.GetMyGraphicsCard.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WebClient webClient;


    @Override
    public String makeSubscription() {
        subscriptionRepository.save(new Subscription());
        return "New subscription made.";
    }

    @Override
    public List<SubscriptionItemDto> getAllSubscribedItems(Long subscriptionId) throws Exception {
        Optional<Subscription> result = subscriptionRepository.findById(subscriptionId);

        if (result.isEmpty()) {
            throw new Exception("Subscription does not exists.");
        }

        log.info("Retrieving all subscribed items");

        return result.get().getSubscriptionItemList().stream().map(this::mapToDto).toList();
    }

    @Override
    public String addItemToSubscription(Long subscriptionId, String id) throws Exception {
        Optional<Subscription> result = subscriptionRepository.findById(subscriptionId);

        if (result.isEmpty()) {
            throw new Exception("Subscription does not exists.");
        }

        log.info("Requesting product with id {} info to product-service", id);
        SubscriptionItem item = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .build(id))
                .retrieve()
                .bodyToMono(SubscriptionItem.class)
                .timeout(Duration.ofSeconds(3))
                .block();
        assert item != null;

       result.get().addItem(item);

        // save to database
        subscriptionRepository.save(result.get());
        String responseMessage = String.format("The requested item %s added to the subscription list.", item.getTitle());
        return responseMessage;
    }

  /*  @Override
    public String deleteItemFromSubscription(Long subscriptionId,  index) throws Exception {
        Optional<Subscription> result = subscriptionRepository.findById(subscription.getId());

        if (result.isEmpty()) {
            throw new Exception("Subscription does not exists.");
        }


     //   List<SubscriptionItem> subscriptionItem = mockSubscription.getSubscriptionItemList().get(index).orElseT;
        String responseMessage = "Item deleted successfully.";
        return responseMessage;
    } */

    private SubscriptionItemDto mapToDto(SubscriptionItem subscriptionItem) {
        return SubscriptionItemDto.builder()
                .title(subscriptionItem.getTitle())
                .link(subscriptionItem.getLink())
                .image(subscriptionItem.getImage())
                .lprice(subscriptionItem.getLprice())
                .build();
    }
}
