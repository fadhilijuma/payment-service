# Payments Service
<img src="https://github.com/fadhilijuma/payment-service/raw/main/funds_transfer.png" >

This service links the white labeled service providers with acquirers ensuring a seamless flow of transactions. 

## Running Locally 
You have to first setup the database that is used as a journal to capture events by running this docker-compose file in development directory:

```dockerfile
docker-compose -f docker-compose-jdbc.yml up 
```
Then run this command in your terminal:
```makefile
make run
```
## Building Locally
```makefile
make build
```

## Building in Docker
Running in docker is achieved by running this docker compose command found in the development directory:

```dockerfile
docker compose up
```
## Architecture

Every transaction is an event, we make use of event sourcing together with CQRS in this service. We have commands that initiate a process and events that capture and update the state.
We commands and events at the model layer and we have a command envelope which helps to wrap the model commands and events. This separation ensures that we have abstracted the data layer from the query layer. 
Each layer can grow individually without interfering with another.

Model Layer Commands:
1. ProcessPayment
2. UpdatePaymentStatus

Model Layer Events:
1. PaymentProcessed
2. PaymentStatusUpdated

Query Layer Commands:
1. PaymentCommandEnvelope
2. GetPayment
3. PaymentUpdateEnvelope

Query Layer Events:
1. CommandProcessed
2. CommandRejected

These commands and events help to capture everything is happening. We can build the current state by doing a projection. This is done via the PaymentEntity which has the event sourcing logic using pekko event sourcing.
We are handling the commands in a Finite State Machine (FSM). We can only handle a ProcessPayment when the state is null or empty. When we move to Payment state, we can no longer receive a new instance of the Entity. We can only handle GetPayment and UpdatePaymentStatus commands.
As you may already know, the commands are handled in the command handler which then executes an event effect. The event handler then returns a new state based on the command handler events. 
When we receive a ProcessPayment command, we initiate the transfer by doing card validations using the Luhn algorithm and then marking the transfer status as PENDING if the validations are successful. Otherwise, the transaction is rejected. 
Once the validations are successful, we mock the response from the acquirer by sending an UpdatePaymentStatus command to self (piping to self).
```java
     context.getLog().debug("Command handled: {}", command);
                        return Effect().persist(events.toJavaList())
                                .thenRun(() -> context.pipeToSelf(futureResult(), (ok, exc) -> {
                                    PaymentCommand.ProcessPayment processPayment = (PaymentCommand.ProcessPayment) command;
                                    PaymentCommand.UpdatePaymentStatus updateCommand = new PaymentCommand.UpdatePaymentStatus(processPayment.paymentId());

                                    return new PaymentEntityCommand.PaymentUpdateEnvelope(updateCommand);

                                }))
                                .thenReply(commandEnvelope.replyTo(), s -> new PaymentEntityResponse.CommandProcessed());
```
We have a function that simply waits for 10 seconds and then fires:
```java
    public CompletionStage<Done> futureResult() {
        CompletableFuture<Done> future = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS)
                .execute(() -> {
                    future.complete(Done.getInstance());
                });

        return future;
    }
```

I wish to mention this function that simply mocks the acquirer and either APPROVES or DECLINES the transaction.

```java
   private Either<PaymentCommandError, List<PaymentEvent>> handleUpdate(
            PaymentCommand.UpdatePaymentStatus command, Clock clock) {
        boolean isOddLastDigit = Integer.parseInt(command.cardNumber().substring(command.cardNumber().length() - 1)) % 2 != 0;
        boolean isApproved = !isOddLastDigit;

        if (isApproved) {
            return right(List.of(new PaymentEvent.PaymentStatusUpdated(command.paymentId(), clock.now(), command.cardNumber(), command.expiryDate(), command.cvv(), command.amount(), command.currency(), command.merchantId(), PaymentStatus.APPROVED)));

        } else {
            return right(List.of(new PaymentEvent.PaymentStatusUpdated(command.paymentId(), clock.now(), command.cardNumber(), command.expiryDate(), command.cvv(), command.amount(), command.currency(), command.merchantId(), PaymentStatus.DECLINED)));

        }
    }
```
I am using Either from vavr library which either returns valid response in right side or error on left side.