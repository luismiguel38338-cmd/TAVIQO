import React, { useEffect, useState } from "react";
import { onAuthStateChanged } from "firebase/auth";
import { collection, onSnapshot, orderBy, query, where } from "firebase/firestore";
import { auth, db, signInGoogle, signOutWeb } from "./firebase";
import "./styles.css";

function TaviqoMark() {
  return (
    <svg width="36" height="36" viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="50" r="44" fill="none" stroke="#00F0FF" strokeWidth="4" />
      <polygon points="23,30 77,30 70,42 30,42" fill="#00F0FF" />
      <polygon points="43,38 57,38 54,74 46,80" fill="#00C2FF" />
    </svg>
  );
}

export default function App() {
  const [user, setUser] = useState(null);
  const [clips, setClips] = useState([]);
  const [streams, setStreams] = useState([]);

  useEffect(() => onAuthStateChanged(auth, setUser), []);

  useEffect(() => {
    if (!user) return;
    const clipsQ = query(
      collection(db, "clips"),
      where("status", "==", "active"),
      orderBy("createdAt", "desc")
    );
    const streamsQ = query(
      collection(db, "live_streams"),
      where("status", "==", "live")
    );
    const unsubClips = onSnapshot(clipsQ, (snap) => {
      setClips(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
    });
    const unsubStreams = onSnapshot(streamsQ, (snap) => {
      setStreams(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
    });
    return () => {
      unsubClips();
      unsubStreams();
    };
  }, [user]);

  return (
    <div className="taviqo-shell">
      <header className="taviqo-header">
        <div className="taviqo-brand-wrap">
          <TaviqoMark />
          <div>
            <span className="taviqo-brand">TAVIQO</span>
            <span className="taviqo-sub">El mundo es tu escenario. • Usuarios 100% Reales</span>
          </div>
        </div>
        {user ? (
          <button className="taviqo-btn-outline" onClick={signOutWeb}>
            Salir ({user.displayName || user.email})
          </button>
        ) : (
          <button className="taviqo-btn-cyan" onClick={signInGoogle}>
            Iniciar sesión con Google
          </button>
        )}
      </header>

      {!user ? (
        <main className="taviqo-hero">
          <h1>El mundo es tu escenario.</h1>
          <p>
            Vídeos cortos 9:16, directos en vivo y regalos virtuales conectados en tiempo real con
            Android e iOS. Cero perfiles falsos, cero interacciones simuladas.
          </p>
        </main>
      ) : (
        <main className="taviqo-grid">
          <section>
            <h2>Feed Vertical Activo ({clips.length})</h2>
            {clips.length === 0 ? (
              <p className="taviqo-muted">Aún no hay clips publicados por usuarios reales.</p>
            ) : (
              clips.map((clip) => (
                <article key={clip.id} className="taviqo-card">
                  <div className="taviqo-author">@{clip.authorUsername}</div>
                  {clip.videoUrl?.startsWith("https://") && (
                    <video src={clip.videoUrl} controls playsInline className="taviqo-video-9-16" />
                  )}
                  <p>{clip.caption}</p>
                  <div className="taviqo-stats">
                    <span>Likes: {clip.likesCount || 0}</span>
                    <span>Comentarios: {clip.commentsCount || 0}</span>
                    <span>Vistas: {clip.viewsCount || 0}</span>
                    <span>Regalos: {clip.giftsCoinsTotal || 0}</span>
                  </div>
                </article>
              ))
            )}
          </section>

          <aside>
            <h2>Directos en Vivo ({streams.length})</h2>
            {streams.length === 0 ? (
              <p className="taviqo-muted">No hay transmisiones activas en este momento.</p>
            ) : (
              streams.map((s) => (
                <div key={s.id} className="taviqo-card">
                  <strong>{s.title}</strong>
                  <p className="taviqo-muted">
                    @{s.hostUsername} • {s.viewerCount || 0} viendo
                  </p>
                </div>
              ))
            )}
          </aside>
        </main>
      )}
    </div>
  );
}
