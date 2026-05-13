export function StatCard({
  label,
  value,
  tone = "cyan",
}: {
  label: string;
  value: string | number;
  tone?: "cyan" | "emerald" | "violet" | "amber";
}) {
  const tones = {
    cyan: "border-cyan-100 bg-cyan-50 text-cyan-700",
    emerald: "border-emerald-100 bg-emerald-50 text-emerald-700",
    violet: "border-violet-100 bg-violet-50 text-violet-700",
    amber: "border-amber-100 bg-amber-50 text-amber-700",
  };

  return (
    <div className="rounded-lg border border-slate-100 bg-white p-5 shadow-sm">
      <div className={`mb-4 inline-flex rounded-md border px-2.5 py-1 text-xs font-bold ${tones[tone]}`}>
        {label}
      </div>
      <p className="text-3xl font-black tracking-tight text-slate-950">{value}</p>
    </div>
  );
}
