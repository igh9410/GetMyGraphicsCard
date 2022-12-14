package com.GetMyGraphicsCard.subscriptionservice.service;

import com.GetMyGraphicsCard.subscriptionservice.dto.SubscriptionItemDto;
import com.GetMyGraphicsCard.subscriptionservice.entity.Subscription;
import com.GetMyGraphicsCard.subscriptionservice.entity.SubscriptionItem;
import com.GetMyGraphicsCard.subscriptionservice.repository.SubscriptionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    public Subscription makeSubscription() {
        Subscription subscription = new Subscription();
        subscriptionRepository.save(subscription);
        return subscription;
    }



    @Override
    public String removeSubscription(Long subscriptionId) {
        Subscription result = subscriptionRepository.getReferenceById(subscriptionId);
        subscriptionRepository.delete(result);
        return "Subscription deleted";
    }

    @Override
    public Subscription findById(Long subscriptionId) {
        return subscriptionRepository.getReferenceById(subscriptionId);
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
    @CircuitBreaker(name = "productService", fallbackMethod = "buildFallbackAddItemToSubscription")
    public SubscriptionItemDto addItemToSubscription(Long subscriptionId, String id) throws Exception {
        Optional<Subscription> result = subscriptionRepository.findById(subscriptionId);

        if (result.isEmpty()) {
            throw new Exception("Please make a subscription to app first");
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
        return mapToDto(item);
    }

    public SubscriptionItemDto buildFallbackAddItemToSubscription(Long subscriptionId, String id, Exception e) {
        SubscriptionItemDto subscriptionItemDto = SubscriptionItemDto.builder()
                .title("Sorry the product service is not available")
                .lprice(0)
                .link("")
                .image("")
                .build();
        return subscriptionItemDto;
    }



    @Override
    public String removeItemFromSubscription(Long subscriptionId,  int index) throws Exception {
        Optional<Subscription> result = subscriptionRepository.findById(subscriptionId);
        if (result.isEmpty()) {
            throw new Exception("Subscription does not exists.");
        }
        SubscriptionItem removedItem = result.get().getSubscriptionItemList().get(index);
        result.get().removeItem(removedItem);

        String responseMessage = "Item deleted successfully.";
        return responseMessage;
    }




    private SubscriptionItemDto mapToDto(SubscriptionItem subscriptionItem) {
        return SubscriptionItemDto.builder()
                .title(subscriptionItem.getTitle())
                .link(subscriptionItem.getLink())
                .image(subscriptionItem.getImage())
                .lprice(subscriptionItem.getLprice())
                .build();
    }
}
