# MtgSmsPricer

MtgSmsPricer will be a utility to help Vanderbilt MTG club members price their Magic: the Gathering cards for trading. Most users seem to consult online sources like TCGPlayer already, so it makes sense to streamline the process for accessing this information. Furthermore, users seem to place high value on the precise balance of card value in a trade, so it will be important for users to be able to easily derive that particular piece of information from the results.  

The application aims to fulfill this need through an SMS interface. Users will send the MtgSmsPricer phone number a card name or list of card names and receive a price or price sum in response. Furthermore, if the user sends two lists of cards delimited by a specific token like an '=?', then the application will respond with two price lists and the calculated difference between these two lists.  

The leverage of the SMS platform is a key component of the application because it means users will be able to consult pricing guides without the need for the internet. This will make it easier to trade when mobile data is weak or users wish to avoid using mobile data as much as possible.  

# Questions:

1. How do you typically determine the value of cards you trade with or trade for?
2. Why do you determine the value of cards this way?
3. How do the people you trade with most often determine the value of their cards?
4. Do you use a price guide (like a website or an app) to determine the value of cards?
5. If you use a price guide, why do you use that particular guide?
6. If you use a price guide, how do you access that particular guide?
7. If you use a price guide, why do you access it in that way?
8. If you do not use a price guide, why not?
9. Do you typically require internet access to guide your trading decisions?
10. How much do you value an equal balance of card prices in the trade?
11. When deciding if a trade is acceptable or not, what do you consider the most important factor in your decision?
12. Why do you consider this the most important factor?

# Answers:

P1 = person 1, P2 = person 2, P3 = person 3

## Question 1:

P1: Online sites with aggregate prices such as TCGPlayer and MTGGoldfish.  
P2: I typically determine the "real" value of cards I purchase by looking up a couple of online vendors and seeing what the going rate is for the card. I'm often willing to trade for a slightly higher than market value for cards in older formats that maintain value extremely well, and possibly a little under value for the new hotness that is likely to lose value within a year when demand for playing the card dips for any reason. However, my personal value obviously depends hugely upon how much I need a card for a deck I am using, but I won't impose this upon the trade value most of the time.  
P3: Starcitygames.com price guide.  

## Question 2:

P1: Sites with aggregate data likely represents a true price for a card within acceptable variance between various online sources.  
P2: This system of valuing cards allows for an impartial metric that is constantly updated to coincide with demand for cards. The caveat about occasionally being willing to adjust value is partially to account for market swings and to foster more goodwill between friends trading for cards that may or may not suddenly change value and create hard feelings.  
P3: The local shop at home used this method.  

## Question 3:

P1: What they like and what they've seen in stores. It feels more like a heuristic than data-driven value.  
P2: Most of my friends look up values on just one website: starcitygames  
P3: Online price gauging.  

## Question 4:

P1: Yes. See answer 1.  
P2: Yes  
P3: Same as answer one.  

## Question 5:

P1: TCGPlayer is the first place I look because they have data on market price as well as current prices if I were to buy a card. Both of these factor into my current value of a card.  
P2: I tend to use TCGPlayer and Star City Games because of their reputability.  
P3: Same as answer two.  

## Question 6:

P1: I use a browser either on my phone or my computer.  
P2: Via the internet.  
P3: iPhone or laptop.  

## Question 7:

P1: Convenience. Phone is particularly useful if I'm not at home.  
P2: The internet is my only way of communicating with SCG's prices on a daily basis to stay current.  
P3: That's the most convenient way.  

## Question 8:

P1: N/A  
P2: N/A  
P3: N/A  

## Question 9:

P1: Yes. Most of my card purchases are online and it makes sense to tie my value to online prices.  
P2: Yes, although between friends I am fine trading cards of approximately equal value if both players are willing to overlook the chance at being slighted.  
P3: Yes, or internet service.  

## Question 10:

P1: Highly. Otherwise, I'd prefer to purchase cards outright.  
P2: Highly between strangers, not as highly between friends, and not as highly if I desperately need a card for a deck.  
P3: I favor equal prices but I'm conscious of upcoming bans or drastic trades as well.  

## Question 11:

P1: Value. Given the trade am I getting at least equal value in terms of price.  
P2: The value of cards and whether I will enjoy my collection more after the trade than before it.  
P3: Necessity, price equality, and inflation potential. in that order.  

## Question 12:

P1: If I'm trying to obtain cards, I usually use online sources and buy cards outright. Incrementally trading for cards is time-consuming and if it comes down to it, selling my cards for cash will likely have a reasonably fast turnaround to obtain the card I want from online sources.  
P2: Enjoyment is the purpose of gaming, but magic is an expensive game and fragile trading is sometimes necessary to grow a collection and have more deck building options.  
P3: Obviously, if I don't need a card, I don't want to go in on a risky trade. If the trade is currently equal, I might have some buyer's remorse. I'm not afraid to nab an unfavorable trade if I suspect a card is going to seriously inflate.  

# Requirements

* Ability to text a card name to the application number and receive a price as a response. This streamlines the workflow that is already in use by users and allows them to gather information without internet access.

* Ability to text a list of prices to the number and receive a price sum as a response. This is another method to streamline the pricing of multiple cards at one time.

* Ability to text two delimited lists of cards and receive prices as a response as well as the difference in price between the two lists. This addresses the users' needs of calculating precise monetary balance in trades.

# Development Approach

...