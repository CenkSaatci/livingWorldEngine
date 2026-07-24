import { useEffect, useRef } from 'react';
import * as THREE from 'three';

export interface DiceSkin {
  name: string;
  faceBg: string;
  dotColor: string;
  numColor: string;
  edgeColor: number;
  meshColor: number;
}

export const DICE_SKINS: DiceSkin[] = [
  { name: 'classic', faceBg: '#f8f4f0', dotColor: '#1a1a2e', numColor: '#1a1a2e', edgeColor: 0x000000, meshColor: 0xf8f4f0 },
  { name: 'cyber', faceBg: '#0a0a1a', dotColor: '#00d2ff', numColor: '#00d2ff', edgeColor: 0x00d2ff, meshColor: 0x0a0a1a },
  { name: 'metal', faceBg: '#2a2a2a', dotColor: '#ffd700', numColor: '#ffd700', edgeColor: 0x888888, meshColor: 0x3a3a3a },
  { name: 'wood', faceBg: '#deb887', dotColor: '#4a2800', numColor: '#4a2800', edgeColor: 0x8b6914, meshColor: 0xdeb887 },
];

// Dot positions for d6
const DOTS: Record<number, [number, number][]> = {
  1: [[0.5, 0.5]],
  2: [[0.25, 0.25], [0.75, 0.75]],
  3: [[0.25, 0.25], [0.5, 0.5], [0.75, 0.75]],
  4: [[0.25, 0.25], [0.75, 0.25], [0.25, 0.75], [0.75, 0.75]],
  5: [[0.25, 0.25], [0.75, 0.25], [0.5, 0.5], [0.25, 0.75], [0.75, 0.75]],
  6: [[0.25, 0.2], [0.75, 0.2], [0.25, 0.5], [0.75, 0.5], [0.25, 0.8], [0.75, 0.8]],
};

function makeFaceTex(value: number, sides: number, skin: DiceSkin): THREE.CanvasTexture {
  const s = 256;
  const c = document.createElement('canvas');
  c.width = s; c.height = s;
  const ctx = c.getContext('2d')!;
  ctx.fillStyle = skin.faceBg;
  ctx.fillRect(0, 0, s, s);

  if (sides === 6 && value >= 1 && value <= 6) {
    ctx.fillStyle = skin.dotColor;
    for (const [px, py] of DOTS[value]) {
      ctx.beginPath();
      ctx.arc(px * s, py * s, 12, 0, Math.PI * 2);
      ctx.fill();
    }
  } else {
    ctx.fillStyle = skin.numColor;
    ctx.font = 'bold 72px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(String(value), s / 2, s / 2);
  }
  return new THREE.CanvasTexture(c);
}

function multiMat(faces: number, sides: number, skin: DiceSkin): THREE.MeshStandardMaterial[] {
  return Array.from({ length: faces }, (_, i) =>
    new THREE.MeshStandardMaterial({
      map: makeFaceTex(i + 1, sides, skin),
      roughness: 0.4, metalness: 0.05,
    })
  );
}

function assignGroups(geo: THREE.BufferGeometry, faces: number, vertsPerFace: number) {
  geo.clearGroups();
  for (let i = 0; i < faces; i++) {
    geo.addGroup(i * vertsPerFace, vertsPerFace, i);
  }
}

// d10: pentagonal dipyramid — 10 triangular faces
function buildD10(skin: DiceSkin): { mesh: THREE.Mesh; faceNormals: THREE.Vector3[] } {
  const h = 0.7, r = 0.45;
  const verts: number[] = [];
  const normals: THREE.Vector3[] = [];

  // top vertex, 5 equator, bottom vertex
  const top = [0, h / 2, 0];
  const bot = [0, -h / 2, 0];
  const eq: number[][] = [];
  for (let i = 0; i < 5; i++) {
    const a = (i / 5) * Math.PI * 2 - Math.PI / 2;
    eq.push([Math.cos(a) * r, 0, Math.sin(a) * r]);
  }

  function addTri(a: number[], b: number[], c: number[], _faceIdx: number) {
    verts.push(...a, ...b, ...c);
    // Compute face normal
    const va = new THREE.Vector3(a[0], a[1], a[2]);
    const vb = new THREE.Vector3(b[0], b[1], b[2]);
    const vc = new THREE.Vector3(c[0], c[1], c[2]);
    const n = new THREE.Triangle(va, vb, vc).getNormal(new THREE.Vector3());
    for (let j = 0; j < 3; j++) normals.push(n.clone());
  }

  // Top half: 5 triangles
  for (let i = 0; i < 5; i++) {
    addTri(top, eq[i], eq[(i + 1) % 5], i);
  }
  // Bottom half: 5 triangles
  for (let i = 0; i < 5; i++) {
    addTri(bot, eq[(i + 1) % 5], eq[i], i + 5);
  }

  const geo = new THREE.BufferGeometry();
  geo.setAttribute('position', new THREE.Float32BufferAttribute(verts, 3));
  geo.setAttribute('normal', new THREE.Float32BufferAttribute(normals.flatMap(n => [n.x, n.y, n.z]), 3));
  geo.computeVertexNormals();
  assignGroups(geo, 10, 3);

  const mats = multiMat(10, 10, skin);
  const mesh = new THREE.Mesh(geo, mats);
  return { mesh, faceNormals: normals };
}

function buildDie(_value: number, sides: number, skin: DiceSkin): { mesh: THREE.Mesh } {
  let mesh: THREE.Mesh;

  switch (sides) {
    case 4: {
      const geo = new THREE.TetrahedronGeometry(0.55);
      assignGroups(geo, 4, 3);
      mesh = new THREE.Mesh(geo, multiMat(4, 4, skin));
      break;
    }
    case 6: {
      const geo = new THREE.BoxGeometry(0.75, 0.75, 0.75);
      mesh = new THREE.Mesh(geo, multiMat(6, 6, skin));
      break;
    }
    case 8: {
      const geo = new THREE.OctahedronGeometry(0.55);
      assignGroups(geo, 8, 3);
      mesh = new THREE.Mesh(geo, multiMat(8, 8, skin));
      break;
    }
    case 10: {
      const r = buildD10(skin);
      mesh = r.mesh;
      break;
    }
    case 12: {
      const geo = new THREE.DodecahedronGeometry(0.55);
      assignGroups(geo, 12, 9);
      mesh = new THREE.Mesh(geo, multiMat(12, 12, skin));
      break;
    }
    case 20: {
      const geo = new THREE.IcosahedronGeometry(0.55);
      assignGroups(geo, 20, 3);
      mesh = new THREE.Mesh(geo, multiMat(20, 20, skin));
      break;
    }
    default: {
      const geo = new THREE.BoxGeometry(0.75, 0.75, 0.75);
      mesh = new THREE.Mesh(geo, multiMat(6, 6, skin));
    }
  }

  return { mesh };
}

interface ThreeDiceResult {
  value: number;
  sides: number;
}

interface Props {
  results: ThreeDiceResult[];
  modifier: number;
  total: number;
  skin?: DiceSkin;
}

interface DieState {
  mesh: THREE.Mesh;
  targetRotation: THREE.Euler;
}

export function ThreeDice({ results, modifier, total, skin = DICE_SKINS[0] }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sceneRef = useRef<{
    scene: THREE.Scene; camera: THREE.PerspectiveCamera;
    renderer: THREE.WebGLRenderer; dice: DieState[]; animId: number;
  } | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const el = containerRef.current;
    const w = el.clientWidth || 400;
    const h = el.clientHeight || 300;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 100);
    camera.position.set(0, 1.5, 6);
    camera.lookAt(0, 0, 0);

    const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    renderer.setSize(w, h);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    el.appendChild(renderer.domElement);

    const ambient = new THREE.AmbientLight(0x404060);
    scene.add(ambient);
    const directional = new THREE.DirectionalLight(0xffffff, 1);
    directional.position.set(2, 5, 3);
    scene.add(directional);

    const dice: DieState[] = [];

    results.forEach((r, i) => {
      const { mesh } = buildDie(r.value, r.sides, skin);
      const offset = (results.length - 1) * 0.7;
      mesh.position.set(i * 1.4 - offset, 0, 0);
      scene.add(mesh);

      // Add edges
      const edgeGeo = new THREE.EdgesGeometry(mesh.geometry);
      const edgeMat = new THREE.LineBasicMaterial({ color: skin.edgeColor, transparent: true, opacity: 0.3 });
      const edges = new THREE.LineSegments(edgeGeo, edgeMat);
      edges.position.copy(mesh.position);
      scene.add(edges);

      // Target rotation: spin such that result face roughly faces camera
      // resultFaceIdx = r.value - 1 (clamped in buildDie)
      const faceAngle = ((r.value - 1) / r.sides) * Math.PI * 2;
      const targetRotation = new THREE.Euler(
        0.3 + Math.random() * 0.4,
        faceAngle,
        0.2 + Math.random() * 0.3,
      );
      mesh.rotation.set(
        Math.random() * Math.PI * 6,
        Math.random() * Math.PI * 6,
        Math.random() * Math.PI * 6,
      );

      dice.push({ mesh, targetRotation });
    });

    const startTime = Date.now();
    const duration = 1500;

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);

      for (const d of dice) {
        d.mesh.rotation.x += (d.targetRotation.x - d.mesh.rotation.x) * 0.06;
        d.mesh.rotation.y += (d.targetRotation.y - d.mesh.rotation.y) * 0.06;
        d.mesh.rotation.z += (d.targetRotation.z - d.mesh.rotation.z) * 0.06;
      }

      renderer.render(scene, camera);
      if (progress < 1) {
        sceneRef.current!.animId = requestAnimationFrame(animate);
      }
    };

    sceneRef.current = { scene, camera, renderer, dice, animId: 0 };
    animate();

    return () => {
      if (sceneRef.current) {
        cancelAnimationFrame(sceneRef.current.animId);
        renderer.dispose();
      }
      if (el.contains(renderer.domElement)) el.removeChild(renderer.domElement);
    };
  }, [results, skin]);

  return (
    <div className="flex flex-col items-center gap-3">
      <div ref={containerRef} className="h-64 w-full" />
      <p className="text-2xl font-heading text-text-primary">
        = {total}{' '}
        {modifier !== 0 && (
          <span className="text-accent text-lg">
            ({modifier > 0 ? '+' : ''}{modifier})
          </span>
        )}
      </p>
    </div>
  );
}
