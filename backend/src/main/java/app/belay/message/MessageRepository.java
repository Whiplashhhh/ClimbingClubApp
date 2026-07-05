package app.belay.message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
            join fetch m.sender
            where m.conversation.id = :conversationId
            order by m.createdAt asc, m.id asc
            """)
    List<Message> findThread(@Param("conversationId") UUID conversationId);

    /** Marque lus les messages d'un fil reçus par l'appelant (envoyés par l'autre, non encore lus). */
    @Modifying
    @Query("""
            update Message m set m.readAt = :now
            where m.conversation.id = :conversationId
              and m.sender.id <> :readerId
              and m.readAt is null
            """)
    int markThreadRead(
            @Param("conversationId") UUID conversationId, @Param("readerId") UUID readerId, @Param("now") Instant now);

    /** Total de messages non lus reçus par l'appelant, tous fils confondus. */
    @Query("""
            select count(m) from Message m
            where (m.conversation.coach.id = :userId or m.conversation.student.id = :userId)
              and m.sender.id <> :userId
              and m.readAt is null
            """)
    long countUnreadForUser(@Param("userId") UUID userId);

    /** Nombre de messages non lus par fil, pour l'appelant (badge par conversation). */
    @Query("""
            select m.conversation.id as conversationId, count(m) as total from Message m
            where m.sender.id <> :userId and m.readAt is null
              and m.conversation.id in :conversationIds
            group by m.conversation.id
            """)
    List<UnreadCount> countUnreadByConversation(
            @Param("userId") UUID userId, @Param("conversationIds") List<UUID> conversationIds);

    /**
     * Dernier message de chaque fil donné (aperçu de liste), corrélé sur {@code createdAt} car les
     * identifiants sont des UUID non ordonnés. Un ex æquo improbable sur l'instant est dédupliqué
     * côté service.
     */
    @Query("""
            select m from Message m
            join fetch m.sender
            where m.conversation.id in :conversationIds
              and m.createdAt = (
                  select max(m2.createdAt) from Message m2
                  where m2.conversation.id = m.conversation.id)
            """)
    List<Message> findLatestPerConversation(@Param("conversationIds") List<UUID> conversationIds);

    interface UnreadCount {
        UUID getConversationId();

        long getTotal();
    }
}
