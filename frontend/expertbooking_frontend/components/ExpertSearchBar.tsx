"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { useDebounce } from "@/hooks/useDebounce";
import type { SpecialtyDto } from "@/types/api";

interface ExpertSearchBarProps {
  specialties: SpecialtyDto[];
}

export function ExpertSearchBar({ specialties }: ExpertSearchBarProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const initialSearch = searchParams.get("search") || "";
  const initialSpecialty = searchParams.get("specialty") || "";

  const [search, setSearch] = useState(initialSearch);
  const [specialty, setSpecialty] = useState(initialSpecialty);

  const debouncedSearch = useDebounce(search, 500);

  useEffect(() => {
    const params = new URLSearchParams(searchParams);
    if (debouncedSearch) {
      params.set("search", debouncedSearch);
    } else {
      params.delete("search");
    }

    if (specialty) {
      params.set("specialty", specialty);
    } else {
      params.delete("specialty");
    }

    router.push(`${pathname}?${params.toString()}`);
  }, [debouncedSearch, specialty, pathname, router, searchParams]);

  return (
    <section className="mb-6 grid gap-3 rounded-lg border border-slate-100 bg-white p-4 shadow-sm lg:grid-cols-[1fr_220px]">
      <input
        type="text"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="rounded-md border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50"
        placeholder="Search by expert name, specialty, or tags"
      />
      <select
        value={specialty}
        onChange={(e) => setSpecialty(e.target.value)}
        title="specialty"
        className="rounded-md border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 outline-none transition focus:border-cyan-400 focus:ring-4 focus:ring-cyan-50"
      >
        <option value="">All specialties</option>
        {specialties.map((spec) => (
          <option key={spec.id} value={spec.slug}>
            {spec.name}
          </option>
        ))}
      </select>
    </section>
  );
}
