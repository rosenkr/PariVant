V1
 Now, im writing a Permissions enum for logged in User (ROLE_USER). 
Its for authorization by request matchers in Spring Security. But i'm not sure on what permissions to define. 
This is critical step even if its simple code. Let me descrbie what I want a logged in (authenticated user) to be authorized to do:
Set their own selections for any upcoming round, and store that as a coupon that only they have access to. 
They'll be accessing their "My page" which is like a dashboard which allows them to: 1. Create coupons (only 1 per round!) 
2. View coupons (which may be registered as loss or win after the underlying round ends). 
3. upload/set own internal probabilities for a round to be used by the model if they run it 
4. (note, THEIR VIEW of that round, not for every website visitor).
4. run the model for an upcoming round.

Username+PW users and OAuth users can live in one User table. (nullable pw)

ChatGpt reply: Instead of assigning permissions to roles,
for managing access to private resources, may be better to have authorization mostly by "ownership"
This seems Role-based access rather than Permission-based
1. endpoints require hasRole("USER")
2. service layer enforces: coupon belongs to current user, probabilities belong to current user, one coupon per user per round, only upcoming rounds can be changed/modelrun

My initial approach is more suited for: p
aid tiers
admins
shared coupons
public profiles
moderation
staff tools

Thus may introduce endpoints:
1. Create coupon: Authenticated? Upcoming round? One coupon per user per round.
2. View coupons: Authenticated? Own coupons? Win/Loss/Undecided status? 
3. Set internal probabilities: Authenticated? Only for own account. Scoped to a round. Only affect that users model input.
4. Run model: Authenticated? Only for upcoming round. Private to user. 

Further questions:
1. What exactly are we creating when we say Coupon? A new entity in DB? or a view of a round with selections? Currently, my actual Round entity
is not modelling loss/win as a status, further hinting at creating a new entity. 
2. Then the "server" will always generate a coupon which matches whatever is it's model selections for each round. (So that Users can compare personal coupons with server-generated ones)
3. How to store the changing of internal probabilities? the point is that the user intends on running a model run on it before creating a coupon for example.
4. How to chck if authenticated? Not only that, but WHAT user that is
5. Is the role ROLE_USER at all necessary? Would we just check the JWT token for authentication and match the Principal object to the relevant User name?
6. Regarding model run, it works by taking in various probability sets,
the idea of the user inputting their own probabilities is to essentially replace the 
internal probability. But then it doesn't make sense to call "EnsembleModel" which
inherently mixes input from external sources to build an internal probability.
Perhaps i would create another implementation of GameModel which reuses (value and picking)
logic from EM but facilititates setting of internal probability. Or maybe this approach:
rename the model to ValueModel then have optional input for internal probability:
either provided (when User wants to), or calculated from the ensemble+market (as server already does)


candidate persistence setup:
user
user_coupon
user_round_probabilities
maybe user_model_run

And important constraints like:

unique (user_id, round_id) on coupon
unique (user_id, round_id) on custom probabilities if only one set per round
all these rows reference the owning user

Second GPT answer:
What you were heading toward with roles/permissions is RBAC. 
What you actually need for “a user may only access their own coupon / own probabilities / 
own model runs” is usually called ownership-based, resource-based, or object-level 
authorization. It is closer to ABAC than classic RBAC, because the decision depends 
on attributes of the resource, such as resource.ownerId == currentUserId, not just on
a static role like ROLE_USER

Essentially: ABAC through PreAuthorize annotations (method-level authorization)
makes more sense in my case
May keep ROLE_USER and impl Permissions/getAuthorities merely for future purpses tho
Can essentially add requestMatchers for anything non-public

Separation between service and preAuthorize: PreSuthorize checks ownership for authed user
by doing "load the resource from the database and compare its owner to the authenticated principal."
but then we may have business logic rules to check in service layer: 