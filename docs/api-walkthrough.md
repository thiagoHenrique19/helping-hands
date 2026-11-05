# API walkthrough

A typical session against a local instance (`docker compose up -d --build`, with `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` set).

```bash
BASE=http://localhost:8080

# 1. An NGO representative creates an account and logs in
curl -s -X POST $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"ngo@hope.org","password":"a-strong-password","role":"ORGANIZATION"}'
TOKEN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ngo@hope.org","password":"a-strong-password"}' | jq -r .accessToken)

# 2. The account registers its organization (and becomes its owner)
curl -s -X POST $BASE/api/organizations -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Hope NGO","email":"contact@hope.org","website":"https://hope.org"}'

# 3. It opens a campaign with a deadline
curl -s -X POST $BASE/api/organizations/1/campaigns -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Food baskets","goalAmount":1000,"category":"FOOD","endsAt":"2030-12-31T23:59:59Z"}'

# 4. Anyone can donate, no account needed
curl -s -X POST $BASE/api/campaigns/1/donations -H 'Content-Type: application/json' \
  -d '{"donorName":"Dave","donorEmail":"dave@mail.com","amount":50,"paymentMethod":"PIX","anonymous":true,"message":"Good luck!"}'

# 5. Public feed and ranking
curl -s "$BASE/api/campaigns/1/donations?size=5"
curl -s "$BASE/api/campaigns/1/top-donors?limit=3"

# 6. The owner reads the impact reports
curl -s -H "Authorization: Bearer $TOKEN" $BASE/api/organizations/1/analytics/dashboard
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/organizations/1/analytics/donations-by-month?from=2026-01-01&to=2026-12-31"
```

## Volunteering
```bash
# Publish an opportunity (owner)
curl -s -X POST $BASE/api/organizations/1/opportunities -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Pack baskets","location":"Sao Paulo","remote":false,"startsAt":"2030-06-01T09:00:00Z","endsAt":"2030-06-01T13:00:00Z","slots":2}'

# A volunteer applies (public)
curl -s -X POST $BASE/api/opportunities/1/applications -H 'Content-Type: application/json' \
  -d '{"volunteerName":"Vera","volunteerEmail":"vera@mail.com","motivation":"I want to help"}'

# The owner approves; when the last slot is taken the opportunity becomes FULL
curl -s -X POST $BASE/api/applications/1/approve -H "Authorization: Bearer $TOKEN"
```
