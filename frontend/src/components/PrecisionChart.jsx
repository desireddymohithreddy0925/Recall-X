import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { monthName } from '../format.js';

/** One point per month. A month with no rated warnings is a gap, not zero. */
export default function PrecisionChart({ data }) {
  const rows = data.map((d) => ({ ...d, label: monthName(d.month) }));
  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={rows} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid stroke="#dde4e9" vertical={false} />
          <XAxis dataKey="label" tick={{ fontSize: 13, fill: '#5b6b78' }} tickLine={false} axisLine={{ stroke: '#cbd5dc' }} />
          <YAxis
            domain={[0, 100]}
            ticks={[0, 25, 50, 75, 100]}
            tickFormatter={(v) => `${v}%`}
            tick={{ fontSize: 13, fill: '#5b6b78' }}
            tickLine={false}
            axisLine={false}
            width={48}
          />
          <Tooltip
            formatter={(value, _name, item) =>
              value == null
                ? ['No rated warnings', 'Precision']
                : [`${value}% (${item.payload.useful} useful, ${item.payload.falsePositive} false positive)`, 'Precision']
            }
          />
          <Line
            type="monotone"
            dataKey="precisionPct"
            stroke="#0e7c66"
            strokeWidth={2.5}
            dot={{ r: 4, fill: '#0e7c66' }}
            connectNulls={false}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
