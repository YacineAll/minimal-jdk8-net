import json
import random
import uuid
from datetime import datetime, timedelta
import time
from kafka import KafkaProducer

# Configuration
KAFKA_BOOTSTRAP_SERVERS = ['localhost:9092']
KAFKA_TOPIC = 'business-events'

# Product types and their subproducts
PRODUCTS = {
    "Loan": ["Mortgage", "Personal", "Auto"],
    "Account": ["Checking", "Savings", "Investment"],
    "Card": ["Credit", "Debit", "Prepaid"],
    "Insurance": ["Home", "Auto", "Life"],
    "Service": ["Advisory", "Transfer", "Payment"]
}

# Function to generate a random ID
def generate_id():
    return str(uuid.uuid4())

# Function to create an event
def create_event(main_id, secondary_ids, event_type, product, subproduct, timestamp, additional_data=None):
    """Create a business event with the given parameters"""
    event = {
        "timestamp": timestamp.isoformat(),
        "mainBusinessObject": {
            "id": main_id,
            "type": {
                "ITRId": event_type
            }
        },
        "secondaryBusinessObjects": [
            {
                "id": sec_id,
                "type": {
                    "ITRId": event_type
                }
            } for sec_id in secondary_ids
        ],
        "product": product,
        "subProduct": subproduct
    }
    
    if additional_data:
        event.update(additional_data)
        
    return event

# Function to generate a set of related events that would cause the duplicate issue
def generate_related_events(case_num):
    """Generate a set of related events that would demonstrate the out-of-order issue"""
    # Select a random product and subproduct
    product = random.choice(list(PRODUCTS.keys()))
    subproduct = random.choice(PRODUCTS[product])
    
    # Generate event type
    event_type = f"{product.upper()}_TYPE"
    
    # Generate base timestamp (simulating events happening around the same time)
    base_timestamp = datetime.now()
    
    # Generate IDs
    main_id = f"MAIN-{case_num}-{generate_id()[:8]}"
    secondary_id_1 = f"SEC1-{case_num}-{generate_id()[:8]}"
    secondary_id_2 = f"SEC2-{case_num}-{generate_id()[:8]}"
    
    # Create three related events that would arrive out of order
    
    # Event A - Initial event with main ID
    event_a = create_event(
        main_id=main_id,
        secondary_ids=[],
        event_type=event_type,
        product=product,
        subproduct=subproduct,
        timestamp=base_timestamp,
        additional_data={"eventName": f"InitialEvent-{case_num}", "amount": random.randint(1000, 10000)}
    )
    
    # Event C - Independent event that should be merged later
    event_c = create_event(
        main_id=secondary_id_2,
        secondary_ids=[],
        event_type=event_type,
        product=product,
        subproduct=subproduct,
        timestamp=base_timestamp + timedelta(seconds=5),
        additional_data={"eventName": f"IndependentEvent-{case_num}", "status": "PENDING"}
    )
    
    # Event B - Linking event that contains references to both previous events
    event_b = create_event(
        main_id=secondary_id_1,
        secondary_ids=[main_id, secondary_id_2],
        event_type=event_type,
        product=product,
        subproduct=subproduct,
        timestamp=base_timestamp + timedelta(seconds=2),
        additional_data={"eventName": f"LinkingEvent-{case_num}", "details": "This event contains the linking information"}
    )
    
    return [event_a, event_b, event_c]

# Function to send events to Kafka
def send_to_kafka(events, producer):
    """Send events to Kafka with deliberate delays to simulate out-of-order arrival"""
    for event in events:
        # Convert event to string
        event_str = json.dumps(event)
        # Send to Kafka
        producer.send(KAFKA_TOPIC, value=event_str.encode('utf-8'))
        producer.flush()
        print(f"Sent event: {event['eventName']} ({event['mainBusinessObject']['id']})")
        # Add small delay
        time.sleep(random.uniform(0.5, 2))

def main():
    # Initialize Kafka producer
    try:
        producer = KafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
        print(f"Connected to Kafka at {KAFKA_BOOTSTRAP_SERVERS}")
    except Exception as e:
        print(f"Failed to connect to Kafka: {e}")
        return
    
    # Generate 5 test cases (15 events total that should result in 5 consolidated objects)
    all_test_events = []
    
    for case_num in range(1, 6):
        related_events = generate_related_events(case_num)
        
        # For testing purposes, deliberately shuffle the events to simulate out-of-order arrival
        # In this example, we'll specifically rearrange for A -> C -> B order
        shuffled_events = [related_events[0], related_events[2], related_events[1]]
        all_test_events.extend(shuffled_events)
    
    # Print summary
    print(f"\nGenerated {len(all_test_events)} events representing 5 business cases.")
    print("In MongoDB, these should initially create 10 documents (duplicates)")
    print("After all events are processed, they should consolidate to 5 documents.")
    
    # Option to save events to file
    with open('test_events.json', 'w') as f:
        json.dump(all_test_events, f, indent=2)
    print("\nEvents saved to test_events.json")
    
    # Uncomment to send events to Kafka
    # print("\nSending events to Kafka...")
    # send_to_kafka(all_test_events, producer)
    # producer.close()

if __name__ == "__main__":
    main()
