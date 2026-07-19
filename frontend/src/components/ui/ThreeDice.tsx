import { useEffect, useRef } from 'react';
import * as THREE from 'three';

const DOT_POSITIONS: Record<number, [number, number][]> = {
  1: [[0.5, 0.5]],
  2: [[0.25, 0.25], [0.75, 0.75]],
  3: [[0.25, 0.25], [0.5, 0.5], [0.75, 0.75]],
  4: [[0.25, 0.25], [0.75, 0.25], [0.25, 0.75], [0.75, 0.75]],
  5: [[0.25, 0.25], [0.75, 0.25], [0.5, 0.5], [0.25, 0.75], [0.75, 0.75]],
  6: [[0.25, 0.2], [0.75, 0.2], [0.25, 0.5], [0.75, 0.5], [0.25, 0.8], [0.75, 0.8]],
};

function dieFaceTexture(value: number, sides: number): THREE.CanvasTexture {
  const canvas = document.createElement('canvas');
  canvas.width = 128;
  canvas.height = 128;
  const ctx = canvas.getContext('2d')!;
  const s = 128;
  ctx.fillStyle = '#f8f4f0';
  ctx.fillRect(0, 0, s, s);
  ctx.strokeStyle = '#ccc';
  ctx.lineWidth = 2;
  ctx.strokeRect(2, 2, s - 4, s - 4);

  if (sides === 6 && value >= 1 && value <= 6) {
    ctx.fillStyle = '#222';
    const dots = DOT_POSITIONS[value];
    for (const [px, py] of dots) {
      ctx.beginPath();
      ctx.arc(px * s, py * s, 8, 0, Math.PI * 2);
      ctx.fill();
    }
  } else {
    ctx.fillStyle = '#333';
    ctx.font = 'bold 48px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(String(value), s / 2, s / 2);
  }
  return new THREE.CanvasTexture(canvas);
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

export function ThreeDice({ results, modifier, total }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sceneRef = useRef<{
    scene: THREE.Scene;
    camera: THREE.PerspectiveCamera;
    renderer: THREE.WebGLRenderer;
    dice: { mesh: THREE.Mesh; value: number; targetRotation: THREE.Euler }[];
    animId: number;
  } | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const el = containerRef.current;
    const w = el.clientWidth || 300;
    const h = el.clientHeight || 200;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 100);
    camera.position.set(0, 2, 6);
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

    const diceData: { mesh: THREE.Mesh; value: number; targetRotation: THREE.Euler }[] = [];

    results.forEach((r, i) => {
      const size = 0.8;
      const geo = new THREE.BoxGeometry(size, size, size);

      const textures = [
        dieFaceTexture(r.value, r.sides),
        dieFaceTexture(r.value, r.sides),
        dieFaceTexture(r.value, r.sides),
        dieFaceTexture(r.value, r.sides),
        dieFaceTexture(r.value, r.sides),
        dieFaceTexture(r.value, r.sides),
      ];
      const materials = textures.map((t) => new THREE.MeshStandardMaterial({
        map: t, roughness: 0.4, metalness: 0.05,
      }));

      const mesh = new THREE.Mesh(geo, materials);
      const offset = (results.length - 1) * 0.6;
      mesh.position.set(i * 1.2 - offset, 0, 0);
      scene.add(mesh);

      const targetRotation = new THREE.Euler(
        Math.random() * Math.PI * 2,
        Math.random() * Math.PI * 2,
        (r.value * (Math.PI * 2)) / 6,
      );
      mesh.rotation.set(
        Math.random() * Math.PI * 4,
        Math.random() * Math.PI * 4,
        Math.random() * Math.PI * 4,
      );
      diceData.push({ mesh, value: r.value, targetRotation });
    });

    const startTime = Date.now();
    const duration = 1500;

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);

      for (const d of diceData) {
        d.mesh.rotation.x += (d.targetRotation.x - d.mesh.rotation.x) * 0.08;
        d.mesh.rotation.y += (d.targetRotation.y - d.mesh.rotation.y) * 0.08;
        d.mesh.rotation.z += (d.targetRotation.z - d.mesh.rotation.z) * 0.08;
      }

      renderer.render(scene, camera);
      if (progress < 1) {
        sceneRef.current!.animId = requestAnimationFrame(animate);
      }
    };

    sceneRef.current = { scene, camera, renderer, dice: diceData, animId: 0 };

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
      <div ref={containerRef} className="h-48 w-full max-w-xs" />
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
