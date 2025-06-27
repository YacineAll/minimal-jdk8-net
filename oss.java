import uuid
import random
from datetime import datetime, timedelta
import json

# Configuration
NUM_TRANSACTIONS = 10            # total transactions to simulate
MIN_EVENTS = 4                   # minimum events per transaction
MAX_EVENTS = 10                  # maximum events per transaction

ASSETS = [
    {
        "name": "A",
        "generates_id": False,
        "stages": ["start", "authorize?", "complete"]
    },
    {
        "name": "B",
        "generates_id": True,
        "stages": ["receive", "validate?", "settle?", "finalize"]
    },
    {
        "name": "C",
        "generates_id": False,
        "stages": ["audit?", "archive"]
    }
]

# Initialize timestamp
SIM_START = datetime.utcnow()
current_time = SIM_START

def next_timestamp():
    """
    Advance current_time by a small random delta and return it.
    """
    global current_time
    delta = timedelta(seconds=random.randint(1, 30))
    current_time += delta
    return current_time.isoformat() + 'Z'


def generate_id(prefix: str) -> str:
    """
    Generate a unique ID string with the given prefix.
    """
    return f"{prefix}_{uuid.uuid4().hex[:8]}"


def simulate_one_transaction(transaction_num: int) -> list:
    """
    Simulate events for a single transaction according to asset pipelines.
    Returns a list of event dicts.
    """
    events = []
    target_count = random.randint(MIN_EVENTS, MAX_EVENTS)

    # Initialize IDs
    prev_main_id = generate_id("TXN")
    payment_id = generate_id("PAY")

    for asset in ASSETS:
        # Determine mainBusinessObjectId for this asset
        if asset["generates_id"]:
            main_id = generate_id("BIZ")
        else:
            main_id = prev_main_id

        # Build secondary references
        secondary = []
        if prev_main_id:
            secondary.append({
                "id": prev_main_id,
                "type": "transaction"
            })
        secondary.append({
            "id": payment_id,
            "type": "payment"
        })

        # Walk stages
        for step in asset["stages"]:
            is_decision = step.endswith("?")
            name = step.rstrip("?")

            # Emit event
            event = {
                "eventId": generate_id("EVT"),
                "timestamp": next_timestamp(),
                "asset": asset["name"],
                "mainBusinessObjectId": main_id,
                "secondaryBusinessObjectRefs": secondary.copy(),
                "stage": name
            }
            events.append(event)

            # If decision, evaluate OK/KO
            if is_decision:
                result = random.choice(["OK", "KO"])
                event["result"] = result
                if result == "KO":
                    return events

            # Stop if reached target count
            if len(events) >= target_count:
                return events

        # Carry forward
        prev_main_id = main_id

    return events


def run_simulation():
    """
    Run the full simulation and output JSON to stdout.
    """
    all_events = []
    for i in range(NUM_TRANSACTIONS):
        tx_events = simulate_one_transaction(i)
        all_events.extend(tx_events)

    # Print or save the events
    print(json.dumps(all_events, indent=2))


if __name__ == "__main__":
    run_simulation()
