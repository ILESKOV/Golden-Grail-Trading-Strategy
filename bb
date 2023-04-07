//@version=4
strategy("Bollinger Bands Strategy with Adjusted Stop Loss, Take Profit, Volatility Filter and Trend Filter", shorttitle="BB Cross adj SL TP VF TF", overlay=true, initial_capital=1000, commission_type=strategy.commission.percent, commission_value=0.02)

// Input parameters
length = input(20, title="Length", type=input.integer, minval=1)
src = input(close, title="Source")
mult = input(2.0, title="Multiplier", type=input.float)
wick_multiplier = input(1.0, title="Wick Multiplier", type=input.float)
start_year = input(2023, title="Start Year", type=input.integer)
start_month = input(4, title="Start Month", type=input.integer)
start_day = input(1, title="Start Day", type=input.integer)
end_year = input(2023, title="End Year", type=input.integer)
end_month = input(4, title="End Month", type=input.integer)
end_day = input(2, title="End Day", type=input.integer)
atr_period = input(14, title="ATR Period", type=input.integer)
atr_threshold = input(0.5, title="ATR Threshold", type=input.float)
trail_points = input(0.5, title="Trail Points", type=input.float)
profit_points = input(1.0, title="Profit Points", type=input.float)
ma_period = input(50, title="MA Period", type=input.integer)

start_ts = timestamp(start_year, start_month, start_day, 0, 0)
end_ts = timestamp(end_year, end_month, end_day, 0, 0)

// Check if the current bar is within the specified date range
within_date_range = (time >= start_ts) and (time <= end_ts)

// Calculate Bollinger Bands
basis = sma(src, length)
dev = mult * stdev(src, length)
upper = basis + dev
lower = basis - dev

// Calculate the stop loss level for long and short positions based on the distance
distance = upper - lower
long_stop_loss = close - trail_points * distance
short_stop_loss = close + trail_points * distance

// Calculate ATR to filter out low volatility periods
atr = atr(atr_period)
is_high_volatility = atr > atr_threshold

// Calculate the MA to filter out trades against the trend
ma = sma(src, ma_period)
is_uptrend = close > ma

// Plot Bollinger Bands, stop loss levels, ATR, and MA on the chart
plot(basis, title="Basis", color=color.blue, linewidth=2)
plot(upper, title="Upper", color=color.red, linewidth=2)
plot(lower, title="Lower", color=color.red, linewidth=2)
plot(long_stop_loss, title="Long Stop Loss", color=color.orange, linewidth=2, style=plot.style_stepline)
plot(short_stop_loss, title="Short Stop Loss", color=color.orange, linewidth=2, style=plot.style_stepline)
plot(atr, title="ATR", color=color.purple, linewidth=2)
plot(ma, title="MA", color=color.green, linewidth=2)

// Calculate price action signals
long_wick_bottom(idx) => low[idx] < lower[idx] and (open[idx] - low[idx]) >= wick_multiplier * abs(close[idx] - open[idx])
short_wick_top(idx) => high[idx] > upper[idx] and (high[idx] - close[idx]) >= wick_multiplier * abs(close[idx] - open[idx])

// Strategy rules
long_entry = src[1] <= lower[1] and long_wick_bottom(1) and within_date_range and is_high_volatility and is_uptrend
long_exit = src[1] >= basis[1] and within_date_range
short_entry = src[1] >= upper[1] and short_wick_top(1) and within_date_range and is_high_volatility and not is_uptrend
short_exit = src[1] <= basis[1] and within_date_range

// Strategy orders
strategy.entry("Long", strategy.long, when=long_entry)
strategy.exit("Long Stop Loss", "Long", stop=long_stop_loss, when=within_date_range)
strategy.order("Long TP", strategy.long, profit=profit_points, when=long_entry and within_date_range)
strategy.close("Long", when=long_exit)

strategy.entry("Short", strategy.short, when=short_entry)
strategy.exit("Short Stop Loss", "Short", stop=short_stop_loss, when=within_date_range)
strategy.order("Short TP", strategy.short, profit=profit_points, when=short_entry and within_date_range)
strategy.close("Short", when=short_exit)
