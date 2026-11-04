import { useCallback, useEffect, useState } from 'react';

/** Carga datos al montar y expone `recargar` para refrescar tras una acción. */
export function useCarga(cargador, avisar) {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);

  const recargar = useCallback(async () => {
    setCargando(true);
    try {
      setDatos(await cargador());
    } catch (error) {
      avisar(error.message, 'error');
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { datos, cargando, recargar };
}
