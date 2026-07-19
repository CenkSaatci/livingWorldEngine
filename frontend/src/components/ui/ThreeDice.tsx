import { useEffect, useRef } from 'react';
import * as THREE from 'three';

function valueSprite(value: number): THREE.Sprite {
  const s = 128;
  const canvas = document.createElement('canvas');
  canvas.width = s;
  canvas.height = s;
  const ctx = canvas.getContext('2d')!;

  ctx.beginPath();
  ctx.arc(s / 2, s / 2, s / 2 - 4, 0, Math.PI * 2);
  ctx.fillStyle = '#fff';
  ctx.fill();
  ctx.strokeStyle = '#ccc';
  ctx.lineWidth = 3;
  ctx.stroke();

  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 64px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(String(value), s / 2, s / 2);

  const tex = new THREE.CanvasTexture(canvas);
  const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthTest: false });
  const sprite = new THREE.Sprite(mat);
  sprite.scale.set(1, 1, 1);
  return sprite;
}

function dotPositions(n: number): [number, number][] {
  switch (n) {
    case 1: return [[0.5, 0.5]];
    case 2: return [[0.3, 0.3], [0.7, 0.7]];
    case 3: return [[0.3, 0.3], [0.5, 0.5], [0.7, 0.7]];
    case 4: return [[0.3, 0.3], [0.7, 0.3], [0.3, 0.7], [0.7, 0.7]];
    case 5: return [[0.3, 0.3], [0.7, 0.3], [0.5, 0.5], [0.3, 0.7], [0.7, 0.7]];
    case 6: return [[0.3, 0.2], [0.7, 0.2], [0.3, 0.5], [0.7, 0.5], [0.3, 0.8], [0.7, 0.8]];
    default: return [[0.5, 0.5]];
  }
}

function faceTexture(value: number, sides: number): THREE.CanvasTexture {
  const s = 256;
  const canvas = document.createElement('canvas');
  canvas.width = s;
  canvas.height = s;
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = '#f8f4f0';
  ctx.fillRect(0, 0, s, s);

  ctx.fillStyle = '#1a1a2e';
  if (sides === 6 && value >= 1 && value <= 6) {
    const dots = dotPositions(value);
    for (const [px, py] of dots) {
      ctx.beginPath();
      ctx.arc(px * s, py * s, 14, 0, Math.PI * 2);
      ctx.fill();
    }
  } else {
    ctx.font = 'bold 72px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(String(value), s / 2, s / 2);
  }
  return new THREE.CanvasTexture(canvas);
}

function createColoredMat(color: string): THREE.MeshStandardMaterial {
  return new THREE.MeshStandardMaterial({ color, roughness: 0.4, metalness: 0.05 });
}

interface DieResult {
  mesh: THREE.Mesh;
  value: number;
  targetRotation: THREE.Euler;
}

interface SceneState {
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  renderer: THREE.WebGLRenderer;
  dice: DieResult[];
  animId: number;
}

interface ThreeDiceResult {
  value: number;
  sides: number;
}

interface Props {
  results: ThreeDiceResult[];
  modifier: number;
  total: number;
}

const COLORS = [0x5bb8c5, 0xe0556b, 0x7ac784, 0xf2a65a, 0xa78bfa];
const COLOR_STRS = ['#5bb8c5', '#e0556b', '#7ac784', '#f2a65a', '#a78bfa'];

function buildDie(value: number, sides: number, color: string): { mesh: THREE.Mesh; sprite: THREE.Sprite } {
  const mat = createColoredMat(color);
  let mesh: THREE.Mesh;

  switch (sides) {
    case 4: {
      const geo = new THREE.TetrahedronGeometry(0.55);
      mesh = new THREE.Mesh(geo, mat);
      break;
    }
    case 6: {
      const tex = faceTexture(value, 6);
      const mats = Array.from({ length: 6 }, () => new THREE.MeshStandardMaterial({
        map: tex, roughness: 0.4, metalness: 0.05,
      }));
      const geo = new THREE.BoxGeometry(0.75, 0.75, 0.75);
      mesh = new THREE.Mesh(geo, mats);
      break;
    }
    case 8: {
      const geo = new THREE.OctahedronGeometry(0.55);
      mesh = new THREE.Mesh(geo, mat);
      break;
    }
    case 10: {
      const geo = new THREE.CylinderGeometry(0.45, 0.3, 0.75, 5, 1);
      mesh = new THREE.Mesh(geo, mat);
      break;
    }
    case 12: {
      const geo = new THREE.DodecahedronGeometry(0.55);
      mesh = new THREE.Mesh(geo, mat);
      break;
    }
    case 20: {
      const geo = new THREE.IcosahedronGeometry(0.55);
      mesh = new THREE.Mesh(geo, mat);
      break;
    }
    default: {
      const geo = new THREE.BoxGeometry(0.75, 0.75, 0.75);
      const tex = faceTexture(value, sides);
      const mats = Array.from({ length: 6 }, () => new THREE.MeshStandardMaterial({
        map: tex, roughness: 0.4, metalness: 0.05,
      }));
      mesh = new THREE.Mesh(geo, mats);
    }
  }

  const sprite = valueSprite(value);
  sprite.position.y = 0.7;

  return { mesh, sprite };
}

export function ThreeDice({ results, modifier, total }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sceneRef = useRef<SceneState | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const el = containerRef.current;
    const w = el.clientWidth || 400;
    const h = el.clientHeight || 300;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 100);
    camera.position.set(0, 1.5, 7);
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

    const dice: DieResult[] = [];

    results.forEach((r, i) => {
      const { mesh, sprite } = buildDie(r.value, r.sides, COLOR_STRS[i % COLORS.length]);
      const offset = (results.length - 1) * 0.75;
      mesh.position.set(i * 1.5 - offset, 0, 0);
      sprite.position.set(i * 1.5 - offset, 0.7, 0);
      scene.add(mesh);
      scene.add(sprite);

      const targetRotation = new THREE.Euler(
        Math.random() * Math.PI * 2,
        Math.random() * Math.PI * 2,
        (r.value * (Math.PI * 2)) / r.sides,
      );
      mesh.rotation.set(
        Math.random() * Math.PI * 4,
        Math.random() * Math.PI * 4,
        Math.random() * Math.PI * 4,
      );
      dice.push({ mesh, value: r.value, targetRotation });
    });

    const startTime = Date.now();
    const duration = 1500;

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);

      for (const d of dice) {
        d.mesh.rotation.x += (d.targetRotation.x - d.mesh.rotation.x) * 0.08;
        d.mesh.rotation.y += (d.targetRotation.y - d.mesh.rotation.y) * 0.08;
        d.mesh.rotation.z += (d.targetRotation.z - d.mesh.rotation.z) * 0.08;
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
  }, [results]);

  return (
    <div className="flex flex-col items-center gap-3">
      <div ref={containerRef} className="h-64 w-full" />
      <p className="text-2xl font-heading text-text-primary">
        = {total}{' '}
        {modifier !== 0 && (
          <span className="text-accent text-lg">
            ({modifier > 0 ? '+' : ''}
            {modifier})
          </span>
        )}
      </p>
    </div>
  );
}
