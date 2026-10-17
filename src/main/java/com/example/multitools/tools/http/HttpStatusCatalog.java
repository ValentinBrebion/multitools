package com.example.multitools.tools.http;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HttpStatusCatalog {

    private static final List<HttpStatusInfo> STATUSES = List.of(
        status(100, "Continue", "Le serveur a reçu les en-têtes et le client peut poursuivre l'envoi de la requête.",
            "Réponse intermédiaire après l'en-tête Expect: 100-continue.",
            "Ce statut est rarement affiché directement à l'utilisateur ; vérifier la suite de l'échange HTTP."),
        status(101, "Switching Protocols", "Le serveur accepte de changer de protocole à la demande du client.",
            "Négociation de WebSocket ou changement de protocole demandé via Upgrade.",
            "Vérifier les en-têtes Upgrade et Connection ainsi que la prise en charge du protocole par le proxy."),
        status(200, "OK", "La requête a abouti.",
            "Il ne s'agit normalement pas d'une erreur ; l'opération demandée a réussi.",
            "Vérifier le corps de réponse si le résultat attendu n'est pas présent."),
        status(201, "Created", "La requête a réussi et une nouvelle ressource a été créée.",
            "Réponse habituelle après la création d'une ressource, souvent via POST ou PUT.",
            "Consulter l'en-tête Location et le corps pour identifier la ressource créée."),
        status(202, "Accepted", "La requête a été acceptée pour traitement, mais celui-ci n'est pas terminé.",
            "Traitement asynchrone ou tâche mise en file d'attente.",
            "Consulter l'URL ou l'identifiant de suivi fourni par l'API et vérifier l'état ultérieurement."),
        status(204, "No Content", "La requête a réussi et la réponse ne contient pas de corps.",
            "Action réussie sans représentation à retourner, par exemple une suppression.",
            "Ne pas attendre de contenu dans le corps ; confirmer que le client gère les réponses vides."),
        status(206, "Partial Content", "Le serveur renvoie une partie de la ressource, à la suite d'une requête Range.",
            "Téléchargement segmenté ou reprise d'un transfert.",
            "Vérifier les en-têtes Range et Content-Range et assembler les segments correctement."),
        status(300, "Multiple Choices", "Plusieurs représentations ou destinations sont possibles.",
            "La ressource existe sous plusieurs variantes ou emplacements.",
            "Examiner les options renvoyées et sélectionner explicitement la représentation attendue."),
        status(301, "Moved Permanently", "La ressource a changé d'adresse de façon permanente.",
            "URL déplacée, changement de domaine ou passage définitif à HTTPS.",
            "Suivre ou mettre à jour l'URL à partir de l'en-tête Location ; vérifier les redirections mises en cache."),
        status(302, "Found", "La ressource est temporairement accessible à une autre adresse.",
            "Redirection temporaire vers une page de connexion, une page d'état ou une URL de remplacement.",
            "Suivre l'en-tête Location et vérifier que le client ne transforme pas à tort la méthode HTTP."),
        status(303, "See Other", "La réponse peut être consultée à une autre adresse, généralement avec GET.",
            "Redirection après une soumission de formulaire ou une opération POST.",
            "Effectuer un GET sur l'adresse Location conformément au comportement attendu."),
        status(304, "Not Modified", "La ressource n'a pas changé depuis la version mise en cache par le client.",
            "Revalidation via If-None-Match ou If-Modified-Since.",
            "Utiliser la copie en cache ; si elle est absente ou périmée, vérifier les validateurs et la configuration du cache."),
        status(307, "Temporary Redirect", "La ressource est temporairement à une autre adresse et la méthode doit être conservée.",
            "Redirection temporaire configurée par l'application, le serveur ou un proxy.",
            "Vérifier Location et répéter la requête à cette adresse avec la même méthode et le même corps."),
        status(308, "Permanent Redirect", "La ressource a changé d'adresse de façon permanente et la méthode doit être conservée.",
            "Redirection permanente d'une API ou d'une route.",
            "Mettre à jour l'URL si approprié et vérifier le comportement des caches et des clients."),
        status(400, "Bad Request", "Le serveur ne peut pas traiter la requête en raison d'une syntaxe ou de données invalides.",
            "JSON mal formé, paramètre manquant ou mal encodé, en-tête invalide, corps trop grand ou syntaxe de requête incorrecte.",
            "Lire le message de réponse et les journaux serveur ; vérifier le format, l'encodage, les paramètres et les en-têtes envoyés."),
        status(401, "Unauthorized", "La requête nécessite une authentification valide ; les identifiants sont absents ou refusés.",
            "Jeton ou identifiants absents, expirés, mal formés, révoqués, ou envoyés dans le mauvais schéma d'authentification.",
            "Vérifier l'en-tête Authorization, renouveler le jeton et consulter WWW-Authenticate pour connaître le mécanisme attendu."),
        status(402, "Payment Required", "Code réservé à des usages futurs, parfois employé par des services pour signaler une exigence de paiement.",
            "Quota ou abonnement épuisé, paiement requis selon les règles du service.",
            "Consulter la documentation et le corps de réponse du service ; ce code n'a pas un usage universel."),
        status(403, "Forbidden", "Le serveur a compris la requête mais refuse l'accès.",
            "Compte sans permission, règle ACL ou pare-feu, restriction IP, ressource protégée ou politique de sécurité.",
            "Vérifier les rôles et permissions, les règles d'accès et les journaux. Contrairement à 401, se réauthentifier ne suffit pas toujours."),
        status(404, "Not Found", "La ressource demandée n'a pas été trouvée, ou le serveur ne souhaite pas révéler son existence.",
            "URL ou identifiant erroné, route absente, ressource supprimée, mauvais préfixe ou configuration de routage incorrecte.",
            "Vérifier l'URL exacte, la méthode, la casse et la route déployée ; contrôler les journaux et l'en-tête Host."),
        status(405, "Method Not Allowed", "La ressource existe mais n'accepte pas la méthode HTTP utilisée.",
            "GET utilisé au lieu de POST, endpoint configuré pour une autre méthode ou route mal associée.",
            "Vérifier la méthode attendue et l'en-tête Allow renvoyé par le serveur."),
        status(406, "Not Acceptable", "Aucune représentation disponible ne correspond aux formats acceptés par le client.",
            "Valeur trop restrictive ou non prise en charge dans Accept, Accept-Language ou un en-tête similaire.",
            "Élargir les en-têtes Accept ou demander un format pris en charge par l'API."),
        status(407, "Proxy Authentication Required", "Le proxy exige une authentification avant de transmettre la requête.",
            "Identifiants de proxy absents, incorrects ou expirés.",
            "Configurer l'authentification du proxy et vérifier Proxy-Authenticate ainsi que les paramètres réseau du client."),
        status(408, "Request Timeout", "Le serveur a attendu trop longtemps la requête complète du client.",
            "Connexion lente ou interrompue, client bloqué, envoi du corps incomplet ou délai serveur dépassé.",
            "Vérifier la connectivité et la taille du corps ; ajuster les délais uniquement si le traitement le justifie."),
        status(409, "Conflict", "La requête entre en conflit avec l'état actuel de la ressource.",
            "Modification concurrente, doublon, contrainte métier ou état incompatible.",
            "Lire le détail de l'API, recharger l'état courant et résoudre le conflit avant de réessayer."),
        status(410, "Gone", "La ressource n'est plus disponible et cette absence est considérée comme définitive.",
            "Ressource ou endpoint retiré intentionnellement.",
            "Supprimer l'ancienne référence et utiliser la nouvelle URL ou version de l'API si elle est indiquée."),
        status(411, "Length Required", "Le serveur exige un en-tête Content-Length pour la requête.",
            "Requête envoyée sans longueur de contenu à un serveur qui ne prend pas en charge le transfert utilisé.",
            "Fournir Content-Length ou utiliser un mode de transfert accepté par le serveur."),
        status(413, "Content Too Large", "Le corps de la requête dépasse la taille que le serveur accepte.",
            "Fichier téléversé trop volumineux ou limite dépassée dans un proxy, serveur web ou framework.",
            "Réduire ou découper le contenu, ou ajuster de façon cohérente les limites de taille sur toute la chaîne."),
        status(415, "Unsupported Media Type", "Le serveur ne prend pas en charge le format du corps envoyé.",
            "Content-Type incorrect ou format non accepté par l'endpoint.",
            "Vérifier Content-Type et encoder le corps dans un format pris en charge, par exemple application/json."),
        status(416, "Range Not Satisfiable", "La plage d'octets demandée ne peut pas être satisfaite.",
            "En-tête Range hors limites ou ressource modifiée depuis la demande de reprise.",
            "Vérifier la taille actuelle de la ressource et l'en-tête Content-Range ; relancer le téléchargement complet si nécessaire."),
        status(417, "Expectation Failed", "Le serveur ne peut pas satisfaire l'attente indiquée dans l'en-tête Expect.",
            "En-tête Expect non pris en charge ou intermédiaire HTTP incompatible.",
            "Retirer ou adapter l'en-tête Expect et vérifier les capacités du serveur et des proxys."),
        status(418, "I'm a teapot", "Code défini comme plaisanterie dans la RFC 2324 ; certains services l'utilisent à leur manière.",
            "Réponse fantaisie ou comportement propre à une API ; ce n'est généralement pas une erreur HTTP applicative standard.",
            "Consulter la documentation du serveur concerné."),
        status(422, "Unprocessable Content", "Le format de la requête est compris, mais son contenu ne peut pas être traité.",
            "Validation métier échouée, champ invalide ou combinaison de données incohérente.",
            "Lire les erreurs de validation dans le corps de réponse et corriger les champs indiqués."),
        status(423, "Locked", "La ressource est verrouillée et ne peut pas être modifiée pour le moment.",
            "Verrou WebDAV ou verrou applicatif détenu par une autre opération.",
            "Identifier le propriétaire ou la durée du verrou et réessayer lorsqu'il est libéré."),
        status(424, "Failed Dependency", "L'action a échoué parce qu'une opération dont elle dépend a échoué.",
            "Échec d'une étape précédente dans une opération liée, notamment WebDAV.",
            "Consulter l'erreur de l'opération dépendante et rétablir son succès avant de relancer."),
        status(426, "Upgrade Required", "Le serveur refuse la requête avec le protocole actuel et demande une mise à niveau.",
            "Version de protocole ou de transport non acceptée.",
            "Vérifier l'en-tête Upgrade et utiliser un protocole pris en charge."),
        status(429, "Too Many Requests", "Le client a envoyé trop de requêtes pendant une période donnée.",
            "Limite de débit ou quota dépassé, rafales de requêtes ou trop de clients partageant la même adresse IP.",
            "Respecter Retry-After, appliquer une temporisation exponentielle avec jitter et réduire la fréquence des appels."),
        status(431, "Request Header Fields Too Large", "Les en-têtes de la requête sont trop volumineux.",
            "Cookies ou jetons excessifs, en-tête répété ou limite du serveur/proxy trop basse.",
            "Réduire et nettoyer les cookies et en-têtes ; vérifier les limites d'en-tête sur les serveurs intermédiaires."),
        status(451, "Unavailable For Legal Reasons", "L'accès à la ressource est bloqué pour des raisons légales.",
            "Restriction légale, réglementaire ou décision applicable à la ressource ou à la région.",
            "Consulter les informations fournies par le service ; une modification de la requête ne résout généralement pas ce blocage."),
        status(500, "Internal Server Error", "Le serveur a rencontré une erreur inattendue sans pouvoir préciser davantage.",
            "Exception non gérée, défaut applicatif, configuration invalide ou dépendance interne en échec.",
            "Consulter les journaux serveur avec l'identifiant de requête, vérifier les changements récents et les services dépendants."),
        status(501, "Not Implemented", "Le serveur ne prend pas en charge la fonctionnalité ou la méthode nécessaire.",
            "Méthode HTTP ou fonctionnalité non implémentée par le serveur ou le proxy.",
            "Vérifier les méthodes et fonctionnalités prises en charge ; ce n'est pas nécessairement un problème temporaire."),
        status(502, "Bad Gateway", "Un serveur agissant comme passerelle a reçu une réponse invalide du serveur amont.",
            "Serveur amont indisponible ou fermé, DNS ou réseau défaillant, réponse amont invalide, TLS ou proxy mal configuré.",
            "Vérifier les journaux du proxy et de l'amont, la résolution DNS, la connectivité, TLS et l'état du service cible."),
        status(503, "Service Unavailable", "Le serveur est temporairement incapable de traiter la requête.",
            "Surcharge, maintenance, capacité épuisée, instance non disponible ou dépendance hors service.",
            "Vérifier l'état de santé et la capacité des instances, consulter Retry-After et réessayer avec temporisation."),
        status(504, "Gateway Timeout", "Une passerelle n'a pas reçu à temps la réponse du serveur amont.",
            "Traitement amont trop lent, amont bloqué ou inaccessible, délai proxy trop court.",
            "Mesurer le temps de réponse de l'amont, vérifier les journaux de chaque saut et optimiser le traitement avant d'augmenter les délais."),
        status(505, "HTTP Version Not Supported", "Le serveur ne prend pas en charge la version HTTP utilisée dans la requête.",
            "Client ou intermédiaire utilisant une version de protocole non acceptée.",
            "Configurer client, proxy et serveur pour utiliser une version HTTP compatible."),
        status(507, "Insufficient Storage", "Le serveur ne dispose pas de l'espace nécessaire pour terminer l'opération.",
            "Espace disque ou quota de stockage épuisé ; code notamment utilisé avec WebDAV.",
            "Vérifier l'espace disponible, les quotas et le nettoyage des données temporaires."),
        status(511, "Network Authentication Required", "Le réseau exige une authentification avant de permettre l'accès.",
            "Portail captif Wi-Fi ou politique d'accès réseau.",
            "S'authentifier via le portail du réseau puis retenter la requête.")
    );

    private HttpStatusCatalog() {
    }

    public static List<HttpStatusInfo> all() {
        return STATUSES;
    }

    public static Optional<HttpStatusInfo> findByCode(int code) {
        return STATUSES.stream().filter(status -> status.code() == code).findFirst();
    }

    public static List<HttpStatusInfo> search(String query) {
        String normalized = query == null ? "" : query.strip().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return STATUSES;
        }
        return STATUSES.stream()
            .filter(status -> status.searchText().contains(normalized))
            .sorted(Comparator.comparingInt(HttpStatusInfo::code))
            .toList();
    }

    private static HttpStatusInfo status(
        int code,
        String phrase,
        String meaning,
        String commonCauses,
        String troubleshooting
    ) {
        return new HttpStatusInfo(
            code,
            phrase,
            meaning,
            List.of(commonCauses.split("(?<=\\.) ")),
            List.of(troubleshooting.split("(?<=\\.) "))
        );
    }
}
