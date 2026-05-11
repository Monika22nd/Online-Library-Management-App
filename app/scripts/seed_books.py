"""Seed the catalog with ~300 books pulled from OpenLibrary.

Run after `alembic upgrade head`:

    python -m app.scripts.seed_books              # default: 300 across 10 subjects
    python -m app.scripts.seed_books --no-enrich  # skip per-work detail fetch
    python -m app.scripts.seed_books --per 50     # 50 per subject => 500 total

Idempotent: books already in the catalog (by openlibrary_work_key) are
skipped, so re-running the script only fills in missing titles.

Strategy:
  * Pull `--per` works for each subject in SEED_SUBJECTS (matches the
    chips on the Home page).
  * Optionally enrich each work via `/works/{id}.json` to grab the
    description, links, excerpts, and a fuller subject list. Enrichment
    is parallelised with a small semaphore so we stay polite.
  * Persist via `books_svc.import_book_with_copies` (one book + one
    edition + three copies per row, plus author linkage).
"""
from __future__ import annotations

import argparse
import asyncio
from typing import Optional

from app.models import SessionLocal
from app.services import books_svc, openlibrary


# Slugs mirrored from `frontend/src/pages/Home.jsx::FEATURED_SUBJECTS`.
# Seeding these guarantees every Home-page chip lands on a non-empty page.
SEED_SUBJECTS: list[str] = [
    "fiction",
    "science_fiction",
    "mystery",
    "history",
    "fantasy",
    "romance",
    "biography",
    "philosophy",
    "art",
    "poetry",
]

# Subject lists from OpenLibrary can run into the hundreds of entries.
# Keep the row light by trimming to a sane top-N before save.
MAX_SUBJECTS_PER_BOOK = 30


def _normalize_subjects(
    subj: list[str] | str | None, slug: str
) -> list[str]:
    """Coerce raw subjects into a list, dedupe, ensure the seeding slug is
    present so the local subject filter can find this book later."""
    out: list[str] = []
    if isinstance(subj, list):
        out = [str(s).strip() for s in subj if s]
    elif isinstance(subj, str) and subj:
        out = [s.strip() for s in subj.split(",") if s.strip()]

    seen: set[str] = set()
    deduped: list[str] = []
    for s in out:
        key = s.lower()
        if key in seen:
            continue
        seen.add(key)
        deduped.append(s)

    slug_label = slug.replace("_", " ")
    if slug_label.lower() not in seen:
        deduped.append(slug_label)
    return deduped[:MAX_SUBJECTS_PER_BOOK]


def _cover_id_from(book: dict) -> Optional[int]:
    if book.get("cover_id"):
        try:
            return int(book["cover_id"])
        except Exception:
            pass
    url = book.get("cover_url") or ""
    if "/id/" in url:
        try:
            return int(url.split("/id/")[1].split("-")[0])
        except Exception:
            return None
    return None


async def _enrich(book: dict, sem: asyncio.Semaphore) -> Optional[dict]:
    """Fetch the work-detail JSON for a single book under a concurrency cap."""
    key = book.get("openlibrary_key")
    if not key:
        return None
    async with sem:
        try:
            return await openlibrary.get_book_details(key)
        except Exception as e:
            print(f"  ! enrich failed for {key}: {e}")
            return None


async def seed_books(
    per_subject: int = 30,
    enrich: bool = True,
    concurrency: int = 6,
) -> int:
    """Pull `per_subject` books for each subject in SEED_SUBJECTS, optionally
    enrich, and persist. Returns the number of books newly inserted."""
    sem = asyncio.Semaphore(concurrency)
    inserted = 0
    inspected = 0

    with SessionLocal() as session:
        for slug in SEED_SUBJECTS:
            print(f"[seed_books] subject={slug} pulling {per_subject}...")
            res = await openlibrary.get_subject_books(slug, limit=per_subject)
            raw_books = res.get("books", []) or []
            inspected += len(raw_books)

            # Skip works already in DB before paying the enrichment cost.
            to_save: list[dict] = []
            for b in raw_books:
                k = b.get("openlibrary_key")
                if not k:
                    continue
                if books_svc.get_book_by_openlibrary_key(session, k):
                    continue
                to_save.append(b)

            details_map: dict[str, dict] = {}
            if enrich and to_save:
                results = await asyncio.gather(
                    *(_enrich(b, sem) for b in to_save),
                    return_exceptions=False,
                )
                for b, d in zip(to_save, results):
                    if d:
                        details_map[b["openlibrary_key"]] = d

            for b in to_save:
                ol_key = b["openlibrary_key"]
                d = details_map.get(ol_key) or {}
                author_refs = d.get("author_refs") or b.get("author_refs") or []
                cover_id = d.get("cover_id") or _cover_id_from(b)
                subjects = _normalize_subjects(
                    d.get("subject_list") or b.get("subjects"), slug
                )
                try:
                    books_svc.import_book_with_copies(
                        session,
                        openlibrary_key=ol_key,
                        title=(b.get("title") or d.get("title") or "Untitled"),
                        cover_id=cover_id,
                        publish_year=b.get("publish_year"),
                        first_publish_date=d.get("first_publish_date"),
                        description=d.get("description"),
                        subjects=subjects,
                        links=d.get("links") or None,
                        excerpts=d.get("excerpts") or None,
                        copies_to_create=3,
                        authors=author_refs,
                    )
                    inserted += 1
                except Exception as e:
                    print(f"  ! save failed for {b.get('title')!r}: {e}")
                    session.rollback()

    print(
        f"[seed_books] inspected={inspected} inserted={inserted} "
        f"(skipped existing: {inspected - inserted})"
    )
    return inserted


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed local catalog from OpenLibrary.")
    parser.add_argument(
        "--per", type=int, default=30,
        help="Books per subject (default 30 across 10 subjects = 300).",
    )
    parser.add_argument(
        "--no-enrich", action="store_true",
        help="Skip /works/{id}.json fetch; faster but no description/excerpts.",
    )
    parser.add_argument(
        "--concurrency", type=int, default=6,
        help="Max in-flight enrichment requests.",
    )
    args = parser.parse_args()

    asyncio.run(seed_books(
        per_subject=args.per,
        enrich=not args.no_enrich,
        concurrency=args.concurrency,
    ))


if __name__ == "__main__":
    main()
