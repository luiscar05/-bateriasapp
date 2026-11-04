import { etiqueta } from '../formato';

export default function Insignia({ valor }) {
  return <span className={`insignia insignia-${valor}`}>{etiqueta(valor)}</span>;
}
