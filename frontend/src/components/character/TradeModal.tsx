import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { X } from 'lucide-react';
import { apiClient } from '../../api/client';
import { useToast } from '../../hooks/useToast';

interface InvItem {
  itemId: string;
  quantity: number;
  name: string;
  type: string;
}

interface EntityRef {
  id: string;
  name: string;
}

interface Trade {
  id: string;
  proposerEntityId: string;
  partnerEntityId: string;
  offerJson: string;
  requestJson: string;
  status: string;
  lastEditorEntityId: string;
  offer?: { itemId: string; quantity: number; name?: string }[];
  request?: { itemId: string; quantity: number; name?: string }[];
}

interface Props {
  entityId: string;
  entityName: string;
  onClose: () => void;
}

const parseItems = (json: string): { itemId: string; quantity: number }[] => {
  try {
    const arr = JSON.parse(json);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
};

/** Handelsfenster (B4): Angebot, Gegenangebot, Annahme, Abbruch. */
export function TradeModal({ entityId, entityName, onClose }: Props) {
  const { t } = useTranslation('common');
  const toast = useToast();
  const [worldId, setWorldId] = useState('');
  const [entities, setEntities] = useState<EntityRef[]>([]);
  const [partnerId, setPartnerId] = useState('');
  const [myInv, setMyInv] = useState<InvItem[]>([]);
  const [partnerInv, setPartnerInv] = useState<InvItem[]>([]);
  const [offer, setOffer] = useState<Record<string, number>>({});
  const [request, setRequest] = useState<Record<string, number>>({});
  const [trades, setTrades] = useState<Trade[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const dialogRef = useRef<HTMLDivElement>(null);
  const loadErrorShown = useRef(false);
  const showLoadError = () => {
    if (loadErrorShown.current) return;
    loadErrorShown.current = true;
    toast.error(t('trade.loadFailed'));
  };

  useEffect(() => {
    apiClient.get(`/entities/${entityId}`).then((res) => {
      const wid = res.data.worldId as string;
      setWorldId(wid);
      apiClient.get(`/worlds/${wid}/entities`).then((r) => {
        setEntities(
          (r.data as (EntityRef & { entityType: string })[])
            .filter((e) => e.id !== entityId && e.entityType !== 'FACTION'),
        );
      }).catch(() => toast.error(t('trade.loadFailed')));
      reloadTrades(wid);
    }).catch(() => toast.error(t('trade.loadFailed')));
    apiClient.get(`/entities/${entityId}/inventory`).then((r) => {
      setMyInv(r.data.items ?? []);
    }).catch(() => toast.error(t('trade.loadFailed')));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityId]);

  const reloadTrades = (wid: string) => {
    apiClient
      .get(`/trades?worldId=${wid}&entityId=${entityId}`)
      .then((r) => setTrades(r.data ?? []))
      .catch(() => showLoadError());
  };

  useEffect(() => {
    if (!partnerId) {
      setPartnerInv([]);
      return;
    }
    apiClient.get(`/entities/${partnerId}/inventory`).then((r) => {
      setPartnerInv(r.data.items ?? []);
    }).catch(() => {
      setPartnerInv([]);
      showLoadError();
    });
  }, [partnerId]);

  const setQty = (
    setter: React.Dispatch<React.SetStateAction<Record<string, number>>>,
    id: string,
    max: number,
    delta: number,
    current: Record<string, number>,
  ) => {
    const next = Math.max(0, Math.min(max, (current[id] ?? 0) + delta));
    setter((prev) => {
      const copy = { ...prev };
      if (next <= 0) delete copy[id];
      else copy[id] = next;
      return copy;
    });
  };

  const toList = (rec: Record<string, number>) =>
    Object.entries(rec).map(([itemId, quantity]) => ({ itemId, quantity }));

  useEffect(() => {
    dialogRef.current?.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const setQtyExact = (
    setter: React.Dispatch<React.SetStateAction<Record<string, number>>>,
    id: string,
    value: number,
  ) => {
    setter((prev) => {
      const copy = { ...prev };
      if (value <= 0) delete copy[id];
      else copy[id] = value;
      return copy;
    });
  };

  const submit = async () => {
    if (!partnerId || saving) return;
    setSaving(true);
    try {
      if (editingId) {
        await apiClient.post(`/trades/${editingId}/counter`, {
          entityId,
          offer: toList(offer),
          request: toList(request),
        });
      } else {
        await apiClient.post('/trades', {
          worldId,
          proposerEntityId: entityId,
          partnerEntityId: partnerId,
          offer: toList(offer),
          request: toList(request),
        });
      }
      setOffer({});
      setRequest({});
      setEditingId(null);
      reloadTrades(worldId);
    } catch {
      toast.error(t('trade.failed'));
    } finally {
      setSaving(false);
    }
  };

  const act = async (id: string, action: 'accept' | 'cancel') => {
    try {
      await apiClient.post(`/trades/${id}/${action}`, { entityId });
      reloadTrades(worldId);
    } catch {
      toast.error(t('trade.failed'));
    }
  };

  const startCounter = (tr: Trade) => {
    const other = tr.proposerEntityId === entityId ? tr.partnerEntityId : tr.proposerEntityId;
    setPartnerId(other);
    // Eigene Seite = Angebot, fremde Seite = Wunsch (aus eigener Sicht gespiegelt).
    const mineIsProposer = tr.proposerEntityId === entityId;
    const myItems = mineIsProposer ? parseItems(tr.offerJson) : parseItems(tr.requestJson);
    const wantItems = mineIsProposer ? parseItems(tr.requestJson) : parseItems(tr.offerJson);
    setOffer(Object.fromEntries(myItems.map((i) => [i.itemId, i.quantity])));
    setRequest(Object.fromEntries(wantItems.map((i) => [i.itemId, i.quantity])));
    setEditingId(tr.id);
  };

  const open = trades.filter((tr) => tr.status === 'proposed');
  const canAccept = (tr: Trade) => tr.lastEditorEntityId !== entityId;

  const nameOf = (id: string) =>
    id === entityId ? entityName : entities.find((e) => e.id === id)?.name ?? id.slice(0, 8);

  const itemName = (id: string) =>
    myInv.find((i) => i.itemId === id)?.name
    ?? partnerInv.find((i) => i.itemId === id)?.name
    ?? id.slice(0, 8);

  const fmtItems = (items: { itemId: string; quantity: number; name?: string }[] | undefined, fallbackJson: string) =>
    (items && items.length > 0
      ? items
      : parseItems(fallbackJson).map((i) => ({ ...i, name: itemName(i.itemId) }))
    ).map((i) => `${i.quantity}× ${i.name ?? itemName(i.itemId)}`).join(', ') || '—';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={t('trade.title')}
    >
      <div
        ref={dialogRef}
        tabIndex={-1}
        className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl border border-bg-elevated bg-bg-surface p-5 shadow-2xl outline-none"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-heading text-text-primary">
            {t('trade.title')} — {entityName}
          </h3>
          <button onClick={onClose} aria-label={t('actions.cancel')}
            className="text-text-secondary hover:text-text-primary">
            <X size={18} />
          </button>
        </div>

        {/* Offene Angebote */}
        {open.length > 0 && (
          <div className="mb-4 space-y-2">
            {open.map((tr) => (
              <div key={tr.id} className="rounded border border-bg-elevated bg-bg-primary/50 p-2 text-xs">
                <p className="text-text-primary">
                  {nameOf(tr.proposerEntityId)} → {nameOf(tr.partnerEntityId)}
                </p>
                <p className="text-text-secondary">
                  {t('trade.gives')}: {fmtItems(tr.offer, tr.offerJson)}
                  {' · '}{t('trade.wants')}: {fmtItems(tr.request, tr.requestJson)}
                </p>
                <div className="mt-1 flex gap-2">
                  {canAccept(tr) && (
                    <button onClick={() => act(tr.id, 'accept')} className="rounded bg-accent px-2 py-0.5 text-white hover:bg-accent/80">
                      {t('trade.accept')}
                    </button>
                  )}
                  <button onClick={() => startCounter(tr)} className="rounded border border-bg-elevated px-2 py-0.5 text-text-secondary hover:text-text-primary">
                    {t('trade.counter')}
                  </button>
                  <button onClick={() => act(tr.id, 'cancel')} className="rounded border border-bg-elevated px-2 py-0.5 text-danger hover:text-danger/70">
                    {t('trade.cancel')}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Neues Angebot / Gegenangebot */}
        <div className="mb-2 flex items-center gap-2">
          <label className="text-xs text-text-secondary">{t('trade.partner')}</label>
          <select
            value={partnerId}
            onChange={(e) => { setPartnerId(e.target.value); setEditingId(null); setOffer({}); setRequest({}); }}
            className="rounded border border-bg-elevated bg-bg-primary px-2 py-1 text-xs text-text-primary outline-none focus:border-accent"
          >
            <option value="">—</option>
            {entities.map((e) => (
              <option key={e.id} value={e.id}>{e.name}</option>
            ))}
          </select>
          {editingId && (
            <span className="rounded bg-accent/10 px-2 py-0.5 text-xs text-accent">{t('trade.countering')}</span>
          )}
        </div>

        {partnerId && (
          <div className="grid grid-cols-2 gap-3">
            <div>
              <p className="mb-1 text-xs font-medium text-text-primary">{t('trade.youGive')}</p>
              {myInv.map((it) => (
                <div key={it.itemId} className="flex items-center gap-1 py-0.5 text-xs">
                  <span className="flex-1 truncate text-text-primary">{it.name} ({it.quantity})</span>
                  <button aria-label={`${t('trade.youGive')} ${it.name} −`} onClick={() => setQty(setOffer, it.itemId, it.quantity, -1, offer)} className="rounded border border-bg-elevated px-1.5 text-text-secondary hover:text-accent">−</button>
                  <span className="w-5 text-center font-mono text-text-primary">{offer[it.itemId] ?? 0}</span>
                  <button aria-label={`${t('trade.youGive')} ${it.name} +`} onClick={() => setQty(setOffer, it.itemId, it.quantity, 1, offer)} className="rounded border border-bg-elevated px-1.5 text-text-secondary hover:text-accent">+</button>
                  <button aria-label={`${t('trade.youGive')} ${it.name} ${t('trade.max')}`} onClick={() => setQtyExact(setOffer, it.itemId, it.quantity)} className="rounded border border-bg-elevated px-1 text-[10px] text-text-secondary hover:text-accent">{t('trade.max')}</button>
                </div>
              ))}
              {myInv.length === 0 && <p className="text-xs text-text-secondary">{t('trade.empty')}</p>}
            </div>
            <div>
              <p className="mb-1 text-xs font-medium text-text-primary">{t('trade.youWant')}</p>
              {partnerInv.map((it) => (
                <div key={it.itemId} className="flex items-center gap-1 py-0.5 text-xs">
                  <span className="flex-1 truncate text-text-primary">{it.name} ({it.quantity})</span>
                  <button aria-label={`${t('trade.youWant')} ${it.name} −`} onClick={() => setQty(setRequest, it.itemId, it.quantity, -1, request)} className="rounded border border-bg-elevated px-1.5 text-text-secondary hover:text-accent">−</button>
                  <span className="w-5 text-center font-mono text-text-primary">{request[it.itemId] ?? 0}</span>
                  <button aria-label={`${t('trade.youWant')} ${it.name} +`} onClick={() => setQty(setRequest, it.itemId, it.quantity, 1, request)} className="rounded border border-bg-elevated px-1.5 text-text-secondary hover:text-accent">+</button>
                  <button aria-label={`${t('trade.youWant')} ${it.name} ${t('trade.max')}`} onClick={() => setQtyExact(setRequest, it.itemId, it.quantity)} className="rounded border border-bg-elevated px-1 text-[10px] text-text-secondary hover:text-accent">{t('trade.max')}</button>
                </div>
              ))}
              {partnerInv.length === 0 && <p className="text-xs text-text-secondary">{t('trade.empty')}</p>}
            </div>
          </div>
        )}

        <div className="mt-4 flex items-center justify-between gap-2">
          <p className="text-xs text-text-secondary">
            {t('trade.summary', {
              give: Object.entries(offer).map(([id, q]) => `${q}× ${itemName(id)}`).join(', ') || '—',
              want: Object.entries(request).map(([id, q]) => `${q}× ${itemName(id)}`).join(', ') || '—',
            })}
          </p>
          <button
            onClick={submit}
            disabled={!partnerId || saving || (Object.keys(offer).length === 0 && Object.keys(request).length === 0)}
            className="rounded bg-accent px-4 py-1.5 text-sm text-white hover:bg-accent/80 disabled:opacity-40"
          >
            {editingId ? t('trade.sendCounter') : t('trade.sendOffer')}
          </button>
        </div>
      </div>
    </div>
  );
}
