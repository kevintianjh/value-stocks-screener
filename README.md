# value-stocks-screener
US stocks screener that filter out companies with at least 5 years of consistent earnings, calculate it's intrinsic value and growth rate, and rank them accordingly

**Problem**: US listed companies with 5 years of consistent growing EPS (earnings per share) could be a good share to invest, but there is over 9000+ stocks in the market, which makes it hard to look into

**Solution**: This app look for companies which have 5 years of consistent positive growing EPS, calculate it's intrinsic value using DCF (EPS) valuation, and ranks them accordingly

**Logic**: <br/>
1) Read the CSV file which contain the huge list of stocks to screen through
2) Query 10 years EPS (earnings per share) data of each stock from API
3) Analyze the EPS data to qualify if it is 5 years consistent growth
4) Calculate the growth rate and intrinsic value
5) Rank them accordingly and output them in a txt file

**Demo**: <br/><br/>
![screen1](https://github.com/kevintianjh/value-stocks-screener/assets/121169051/fdf1d808-7bdd-4b7a-944d-2c13af95b6cc)
<br/><br/>
1) The process of downloading EPS data for 1000s of stocks can take a while, the app will save the downloaded data in a temporary JSON file (https://github.com/kevintianjh/value-stocks-screener/blob/main/output/temp.json) for any incomplete run
<br/><br/>
![screen4](https://github.com/kevintianjh/value-stocks-screener/assets/121169051/bd287419-db0e-4821-9c4f-2c068d0d192a)
<br/><br/>
![screen2](https://github.com/kevintianjh/value-stocks-screener/assets/121169051/e6d47e26-9eb0-4c4c-930a-81767909b8e5)
<br/><br/>
2) Some EPS data downloaded are in different currency. The app will prompt user to enter the exchange rates
<br/><br/>
![screen3](https://github.com/kevintianjh/value-stocks-screener/assets/121169051/6b31c977-bc1e-4417-ae66-0f7375a8895f)
3) The result are generated and saved in a text file
<br/><br/>
![screen5](https://github.com/kevintianjh/value-stocks-screener/assets/121169051/fe3b98a7-823f-4035-80c3-0faa718a7fc9)
<br/><br/>
4) The text file (https://github.com/kevintianjh/value-stocks-screener/blob/main/output/result.txt) shows the scan results sorted by sector, price/intrinsic (value with equal or less than 100% will be rank 1, 100% to 110% will be rank 2, anything above will be rank 3) and growth rate





