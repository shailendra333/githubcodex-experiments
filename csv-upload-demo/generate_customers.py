"""
generate_customers.py
---------------------
Generates a large sample CSV file for testing the CSV upload demo.

Usage:
    python generate_customers.py --rows 100000 --output customers_100k.csv
"""

import csv
import random
import argparse
from datetime import date, timedelta

FIRST_NAMES = ["James","Mary","John","Patricia","Robert","Jennifer","Michael",
               "Linda","William","Barbara","David","Susan","Richard","Jessica",
               "Joseph","Sarah","Thomas","Karen","Charles","Lisa","Emma","Noah"]

LAST_NAMES  = ["Smith","Johnson","Williams","Brown","Jones","Garcia","Miller",
               "Davis","Rodriguez","Martinez","Hernandez","Lopez","Gonzalez",
               "Wilson","Anderson","Taylor","Thomas","Moore","Jackson","Martin"]

CITIES = [
    ("New York","US"), ("Los Angeles","US"), ("London","GB"), ("Toronto","CA"),
    ("Sydney","AU"), ("Berlin","DE"), ("Paris","FR"), ("Tokyo","JP"),
    ("Mumbai","IN"), ("São Paulo","BR"), ("Dubai","AE"), ("Singapore","SG"),
]

def random_date(start_year=2018, end_year=2024):
    start = date(start_year, 1, 1)
    end   = date(end_year, 12, 31)
    return start + timedelta(days=random.randint(0, (end - start).days))

def generate(rows: int, output: str):
    with open(output, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["externalId","firstName","lastName","email",
                         "phone","city","country","registrationDate"])
        for i in range(1, rows + 1):
            fn   = random.choice(FIRST_NAMES)
            ln   = random.choice(LAST_NAMES)
            city, country = random.choice(CITIES)
            email = f"{fn.lower()}.{ln.lower()}{i}@example.com"
            phone = f"555-{random.randint(1000,9999)}"
            reg   = random_date()
            writer.writerow([i, fn, ln, email, phone, city, country, reg])
            if i % 50_000 == 0:
                print(f"  Generated {i:,} rows…")
    print(f"✅ Done — {rows:,} rows written to {output}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--rows",   type=int, default=100_000)
    parser.add_argument("--output", type=str, default="customers_100k.csv")
    args = parser.parse_args()
    print(f"Generating {args.rows:,} customer rows → {args.output}")
    generate(args.rows, args.output)

