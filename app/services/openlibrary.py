import httpx
import json
from typing import Optional, List, Dict, Any

OPENLIBRARY_API = "https://openlibrary.org"

async def search_books(query: str, limit: int = 20, page: int = 1) -> Dict:
    url = f"{OPENLIBRARY_API}/search.json"
    params = {
        "q": query,
        "limit": limit,
        "offset": (page - 1) * limit,
        "fields": "key,title,author_name,author_key,isbn,cover_i,first_publish_year,subject,edition_count,language"
    }

    async with httpx.AsyncClient() as client:
        try:
            resp = await client.get(url, params=params, timeout=15.0)
            resp.raise_for_status()
            data = resp.json()

            books = []
            for doc in data.get("docs", []):
                cover_url = None
                if doc.get("cover_i"):
                    cover_url = f"https://covers.openlibrary.org/b/id/{doc['cover_i']}-M.jpg"

                subjects = doc.get("subject", [])
                if isinstance(subjects, list):
                    subjects = ", ".join(subjects[:5])

                author_names = doc.get("author_name") or []
                author_keys = doc.get("author_key") or []
                author_refs = [
                    {"key": f"/authors/{k}", "name": n}
                    for k, n in zip(author_keys, author_names)
                ]

                books.append({
                    "openlibrary_key": doc.get("key"),
                    "title": doc.get("title"),
                    "author": author_names[0] if author_names else "Unknown",
                    "authors": author_names if author_names else ["Unknown"],
                    "author_refs": author_refs,
                    "isbn": doc.get("isbn", [None])[0] if doc.get("isbn") else None,
                    "cover_url": cover_url,
                    "publish_year": doc.get("first_publish_year"),
                    "subjects": subjects,
                    "edition_count": doc.get("edition_count", 0),
                })
            
            return {
                "books": books,
                "total": data.get("numFound", 0),
                "page": page,
                "limit": limit,
            }
        except Exception as e:
            print(f"OpenLibrary API error: {e}")
            return {"books": [], "total": 0, "page": page, "limit": limit}


async def get_book_details(openlibrary_key: str) -> Optional[Dict]:
    """Get detailed book info from OpenLibrary works API."""
    url = f"{OPENLIBRARY_API}{openlibrary_key}.json"
    
    async with httpx.AsyncClient() as client:
        try:
            resp = await client.get(url, timeout=15.0)
            resp.raise_for_status()
            data = resp.json()
            
            description = data.get("description")
            if isinstance(description, dict):
                description = description.get("value", "")
            
            subjects = data.get("subjects", [])
            subject_list = subjects[:10] if isinstance(subjects, list) else []
            subjects_str = ", ".join(subject_list) if subject_list else ""
            
            cover_url = None
            if data.get("covers"):
                cover_id = data["covers"][0]
                cover_url = f"https://covers.openlibrary.org/b/id/{cover_id}-L.jpg"
            
            # Get author details
            authors: list[str] = []
            author_refs: list[Dict[str, str]] = []
            for author_ref in data.get("authors", []):
                author_key = None
                if isinstance(author_ref, dict):
                    author_key = author_ref.get("author", {}).get("key", author_ref.get("key"))
                if author_key:
                    try:
                        author_resp = await client.get(f"{OPENLIBRARY_API}{author_key}.json", timeout=10.0)
                        if author_resp.status_code == 200:
                            author_data = author_resp.json()
                            name = author_data.get("name", "Unknown")
                            authors.append(name)
                            author_refs.append({"key": author_key, "name": name})
                    except:
                        pass

            cover_id = data["covers"][0] if data.get("covers") else None

            return {
                "openlibrary_key": openlibrary_key,
                "title": data.get("title", "Unknown"),
                "description": description or "No description available.",
                "subjects": subjects_str,
                "subject_list": subject_list,
                "cover_url": cover_url,
                "cover_id": cover_id,
                "authors": authors if authors else ["Unknown"],
                "author_refs": author_refs,
                "first_publish_date": data.get("first_publish_date"),
                "created": data.get("created", {}).get("value") if isinstance(data.get("created"), dict) else None,
                "links": data.get("links", []),
                "excerpts": data.get("excerpts", []),
            }
        except Exception as e:
            print(f"OpenLibrary API error: {e}")
            return None


async def get_trending_books(limit: int = 20) -> List[Dict]:
    """Get trending/popular books from OpenLibrary."""
    url = f"{OPENLIBRARY_API}/trending/daily.json"
    
    async with httpx.AsyncClient() as client:
        try:
            resp = await client.get(url, params={"limit": limit}, timeout=15.0)
            resp.raise_for_status()
            data = resp.json()
            
            books = []
            for work in data.get("works", [])[:limit]:
                cover_url = None
                if work.get("cover_i"):
                    cover_url = f"https://covers.openlibrary.org/b/id/{work['cover_i']}-M.jpg"
                elif work.get("cover_id"):
                    cover_url = f"https://covers.openlibrary.org/b/id/{work['cover_id']}-M.jpg"
                
                books.append({
                    "openlibrary_key": work.get("key"),
                    "title": work.get("title"),
                    "author": work.get("author_name", ["Unknown"])[0] if work.get("author_name") else "Unknown",
                    "authors": work.get("author_name", ["Unknown"]),
                    "cover_url": cover_url,
                    "publish_year": work.get("first_publish_year"),
                    "edition_count": work.get("edition_count", 0),
                })
            return books
        except Exception as e:
            print(f"OpenLibrary trending API error: {e}")
            return []


async def get_subject_books(subject: str, limit: int = 20, offset: int = 0) -> Dict:
    """Get books by subject from OpenLibrary."""
    url = f"{OPENLIBRARY_API}/subjects/{subject}.json"
    
    async with httpx.AsyncClient() as client:
        try:
            resp = await client.get(url, params={"limit": limit, "offset": offset}, timeout=15.0)
            resp.raise_for_status()
            data = resp.json()
            
            books = []
            for work in data.get("works", []):
                cover_url = None
                if work.get("cover_id"):
                    cover_url = f"https://covers.openlibrary.org/b/id/{work['cover_id']}-M.jpg"
                
                authors = [a.get("name", "Unknown") for a in work.get("authors", [])]
                
                books.append({
                    "openlibrary_key": work.get("key"),
                    "title": work.get("title"),
                    "author": authors[0] if authors else "Unknown",
                    "authors": authors if authors else ["Unknown"],
                    "cover_url": cover_url,
                    "publish_year": work.get("first_publish_year"),
                    "edition_count": work.get("edition_count", 0),
                    "subjects": work.get("subject", []),
                })
            
            return {
                "books": books,
                "total": data.get("work_count", 0),
                "subject": data.get("name", subject),
            }
        except Exception as e:
            print(f"OpenLibrary subject API error: {e}")
            return {"books": [], "total": 0, "subject": subject}


async def get_author_details(author_key: str) -> Optional[Dict]:
    """Get author details from OpenLibrary."""
    url = f"{OPENLIBRARY_API}/authors/{author_key}.json"
    
    async with httpx.AsyncClient() as client:
        try:
            resp = await client.get(url, timeout=15.0)
            resp.raise_for_status()
            data = resp.json()
            
            photo_url = None
            if data.get("photos"):
                photo_url = f"https://covers.openlibrary.org/a/id/{data['photos'][0]}-L.jpg"
            
            bio = data.get("bio")
            if isinstance(bio, dict):
                bio = bio.get("value", "")
            
            return {
                "key": author_key,
                "name": data.get("name", "Unknown"),
                "bio": bio,
                "birth_date": data.get("birth_date"),
                "death_date": data.get("death_date"),
                "photo_url": photo_url,
                "wikipedia": data.get("wikipedia"),
            }
        except Exception as e:
            print(f"OpenLibrary author API error: {e}")
            return None


async def fetch_and_save_books(session, query: str = "programming", limit: int = 50) -> int:
    """Search OpenLibrary and persist each result as a Book + Edition + 3 Copies.
    `session` is a SQLAlchemy Session injected by the route."""
    from app.services import books_svc

    result = await search_books(query, limit)
    books = result.get("books", [])
    saved = 0

    for book in books:
        try:
            ol_key = book["openlibrary_key"]
            if not ol_key:
                continue
            existing = books_svc.get_book_by_openlibrary_key(session, ol_key)
            if existing:
                continue

            cover_id = None
            if book.get("cover_url"):
                # cover_url looks like .../b/id/{cover_id}-M.jpg
                try:
                    cover_id = int(book["cover_url"].split("/id/")[1].split("-")[0])
                except Exception:
                    cover_id = None

            subjects_list = None
            if isinstance(book.get("subjects"), str) and book["subjects"]:
                subjects_list = [s.strip() for s in book["subjects"].split(",")]

            books_svc.import_book_with_copies(
                session,
                openlibrary_key=ol_key,
                title=book["title"] or "Untitled",
                cover_id=cover_id,
                publish_year=book.get("publish_year"),
                subjects=subjects_list,
                isbn=book.get("isbn"),
                copies_to_create=3,
                authors=book.get("author_refs"),
            )
            saved += 1
        except Exception as e:
            print(f"Error saving book {book.get('title')}: {e}")

    return saved


async def import_one_by_work_key(session, work_key: str) -> Optional[int]:
    """Import a single OpenLibrary work into the local catalog. Idempotent.
    Returns the local books.book_id, or None if the work could not be loaded."""
    from app.services import books_svc

    # Normalize: accept "OL...W" or "/works/OL...W"
    key = work_key.strip()
    if not key.startswith("/works/"):
        key = f"/works/{key.lstrip('/')}"

    existing = books_svc.get_book_by_openlibrary_key(session, key)
    if existing:
        return existing.book_id

    details = await get_book_details(key)
    if not details:
        return None

    subjects_list = details.get("subject_list") or None
    book = books_svc.import_book_with_copies(
        session,
        openlibrary_key=key,
        title=details.get("title") or "Untitled",
        cover_id=details.get("cover_id"),
        subjects=subjects_list,
        copies_to_create=3,
        authors=details.get("author_refs"),
    )
    return book.book_id if book else None